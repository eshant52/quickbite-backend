package com.quickbite.quickbite.order.scheduler;

import com.quickbite.quickbite.common.config.property.OrderProperties;
import com.quickbite.quickbite.order.service.OrderLifecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Background scheduler that periodically sweeps stale orders in {@code AWAITING_PAYMENT}
 * status that have exceeded the payment TTL (default 15 minutes) and marks them {@code ABANDONED}.
 *
 * <p>Distributed Concurrency Safeguards:
 * <ul>
 *   <li><b>PostgreSQL {@code FOR UPDATE SKIP LOCKED}:</b> Rather than relying on a single-leader
 *       distributed lock (which causes non-leader pods to idle), every application instance
 *       can run this sweep concurrently. PostgreSQL's {@code SKIP LOCKED} guarantees that each pod
 *       exclusively locks a distinct chunk of orders without waiting or colliding.</li>
 *   <li><b>Chunked Isolated Transactions:</b> Each batch of up to {@code batchSize} orders is fetched
 *       and processed within an isolated {@code REQUIRES_NEW} transaction via
 *       {@link OrderLifecycleService#processAbandonmentBatch}. Row locks are released as soon as
 *       each batch commits.</li>
 *   <li><b>Idempotent Abandonment:</b> {@link OrderLifecycleService#abandonOrder} includes a
 *       terminal-state guard that silently skips orders transitioned concurrently by payments
 *       or customer cancellations.</li>
 *   <li><b>PostgreSQL Partial Index:</b> Accelerated by the partial index on
 *       {@code (created_at) WHERE current_status = 'AWAITING_PAYMENT'}.</li>
 * </ul>
 */
@Component
public class OrderAbandonmentScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderAbandonmentScheduler.class);
    private static final int MAX_BATCHES_PER_RUN = 10;

    private final OrderLifecycleService orderLifecycleService;
    private final OrderProperties orderProperties;

    public OrderAbandonmentScheduler(
            OrderLifecycleService orderLifecycleService,
            OrderProperties orderProperties) {
        this.orderLifecycleService = orderLifecycleService;
        this.orderProperties = orderProperties;
    }

    @Scheduled(cron = "${quickbite.order.abandon-cron:0 */5 * * * *}")
    public void sweepAbandonedOrders() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(orderProperties.abandonTtlMinutes()));
        log.info("Starting distributed order abandonment sweep (SKIP LOCKED) for orders older than {}", cutoff);

        int totalAbandoned = 0;
        try {
            for (int batch = 0; batch < MAX_BATCHES_PER_RUN; batch++) {
                int processedInBatch = orderLifecycleService.processAbandonmentBatch(cutoff, orderProperties.abandonBatchSize());
                totalAbandoned += processedInBatch;

                // If fewer than batchSize orders were locked and processed, no more stale orders remain
                if (processedInBatch < orderProperties.abandonBatchSize()) {
                    break;
                }
            }

            log.info("Order abandonment sweep completed on this node. Total orders processed: {}", totalAbandoned);
        } catch (Exception e) {
            log.error("Error occurred during order abandonment sweep on this node: {}", e.getMessage(), e);
        }
    }
}
