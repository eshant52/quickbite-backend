package com.quickbite.quickbite.order.scheduler;

import com.quickbite.quickbite.order.service.OrderLifecycleService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Distributed background safety-net scheduler that periodically sweeps unaccepted orders in {@code PLACED}
 * status whose {@code restaurantAcceptanceDeadline} has elapsed, marks them {@code CANCELLED}
 * with reason {@code RESTAURANT_UNRESPONSIVE}, and issues refunds for captured online payments.
 *
 * <p>Primary timeout dispatch is handled in real time by {@link com.quickbite.quickbite.order.consumer.OrderTimeoutConsumer}
 * via Redisson delayed queue. This scheduler acts as a low-frequency distributed safety net.
 *
 * <p>Concurrency Safeguards:
 * <ul>
 *   <li><b>Distributed Lock:</b> Uses Redisson {@code tryLock(0)} to ensure only one pod executes the sweep.</li>
 *   <li><b>Non-Locking ID Fetch:</b> Fetches candidate IDs without holding batch-wide locks.</li>
 *   <li><b>Isolated Per-Order Transactions:</b> Each unaccepted order is locked and cancelled in its own
 *       isolated {@code REQUIRES_NEW} transaction via {@link OrderLifecycleService#cancelUnacceptedOrder}.</li>
 * </ul>
 */
@Component
public class RestaurantAcceptanceTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(RestaurantAcceptanceTimeoutScheduler.class);
    private static final String LOCK_KEY = "quickbite:lock:scheduler:restaurant-acceptance-safety-net";
    private static final int LEASE_TIME_SECONDS = 60;
    private static final int MAX_BATCHES_PER_RUN = 10;
    private static final int BATCH_SIZE = 50;

    private final OrderLifecycleService orderLifecycleService;
    private final RedissonClient redissonClient;

    public RestaurantAcceptanceTimeoutScheduler(
            OrderLifecycleService orderLifecycleService,
            RedissonClient redissonClient) {
        this.orderLifecycleService = orderLifecycleService;
        this.redissonClient = redissonClient;
    }

    @Scheduled(fixedDelayString = "${quickbite.order.acceptance-poll-interval-ms:1800000}")
    public void sweepUnacceptedOrders() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        boolean acquired;
        try {
            acquired = lock.tryLock(0, LEASE_TIME_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        if (!acquired) {
            log.debug("Another pod is running restaurant acceptance timeout safety-net sweep; skipping.");
            return;
        }

        try {
            Instant now = Instant.now();
            int totalProcessed = 0;

            for (int batch = 0; batch < MAX_BATCHES_PER_RUN; batch++) {
                int processedInBatch = orderLifecycleService.processRestaurantAcceptanceTimeoutBatch(now, BATCH_SIZE);
                totalProcessed += processedInBatch;

                if (processedInBatch < BATCH_SIZE) {
                    break;
                }
            }

            if (totalProcessed > 0) {
                log.info("Restaurant acceptance timeout safety-net sweep completed. Cancelled {} unaccepted orders.", totalProcessed);
            }
        } catch (Exception e) {
            log.error("Error during restaurant acceptance timeout safety-net sweep: {}", e.getMessage(), e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
