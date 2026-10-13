package com.quickbite.quickbite.order.scheduler;

import com.quickbite.quickbite.common.config.property.OrderProperties;
import com.quickbite.quickbite.order.service.OrderLifecycleService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Background safety-net scheduler that periodically sweeps stale orders in {@code AWAITING_PAYMENT}
 * status that have exceeded the payment TTL (default 15 minutes) and marks them {@code ABANDONED}.
 *
 * <p>Primary abandonment dispatch is handled in real time by {@link com.quickbite.quickbite.order.consumer.OrderTimeoutConsumer}
 * via Redisson delayed queue. This scheduler acts as a low-frequency distributed safety net.
 *
 * <p>Distributed Concurrency Safeguards:
 * <ul>
 *   <li><b>Distributed Lock:</b> Uses Redisson {@code tryLock(0)} to ensure only one pod executes the sweep.</li>
 *   <li><b>Non-Locking ID Fetch:</b> Fetches stale candidate IDs without holding batch-wide locks.</li>
 *   <li><b>Isolated Per-Order Transactions:</b> Each stale order is locked and abandoned in its own
 *       isolated {@code REQUIRES_NEW} transaction via {@link OrderLifecycleService#abandonOrderById},
 *       minimizing row-lock duration and preventing cross-order rollback.</li>
 *   <li><b>Idempotent Abandonment:</b> {@link OrderLifecycleService#abandonOrder} includes a
 *       terminal-state guard that silently skips orders transitioned concurrently by payments
 *       or customer cancellations.</li>
 * </ul>
 */
@Component
public class OrderAbandonmentScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderAbandonmentScheduler.class);
    private static final String LOCK_KEY = "quickbite:lock:scheduler:order-abandonment-safety-net";
    private static final int LEASE_TIME_SECONDS = 120;
    private static final int MAX_BATCHES_PER_RUN = 10;

    private final OrderLifecycleService orderLifecycleService;
    private final OrderProperties orderProperties;
    private final RedissonClient redissonClient;

    public OrderAbandonmentScheduler(
            OrderLifecycleService orderLifecycleService,
            OrderProperties orderProperties,
            RedissonClient redissonClient) {
        this.orderLifecycleService = orderLifecycleService;
        this.orderProperties = orderProperties;
        this.redissonClient = redissonClient;
    }

    @Scheduled(cron = "${quickbite.order.abandon-cron:0 0 * * * *}")
    public void sweepAbandonedOrders() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        boolean acquired;
        try {
            acquired = lock.tryLock(0, LEASE_TIME_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        if (!acquired) {
            log.debug("Another pod is running order abandonment safety-net sweep; skipping.");
            return;
        }

        try {
            Instant cutoff = Instant.now().minus(Duration.ofMinutes(orderProperties.abandonTtlMinutes()));
            log.info("Starting distributed order abandonment safety-net sweep for orders older than {}", cutoff);

            int totalAbandoned = 0;
            for (int batch = 0; batch < MAX_BATCHES_PER_RUN; batch++) {
                int processedInBatch = orderLifecycleService.processAbandonmentBatch(cutoff, orderProperties.abandonBatchSize());
                totalAbandoned += processedInBatch;

                // If fewer than batchSize orders were locked and processed, no more stale orders remain
                if (processedInBatch < orderProperties.abandonBatchSize()) {
                    break;
                }
            }

            log.info("Order abandonment safety-net sweep completed. Total orders processed: {}", totalAbandoned);
        } catch (Exception e) {
            log.error("Error occurred during order abandonment safety-net sweep: {}", e.getMessage(), e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
