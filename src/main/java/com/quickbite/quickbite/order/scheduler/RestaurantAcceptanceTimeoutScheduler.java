package com.quickbite.quickbite.order.scheduler;

import com.quickbite.quickbite.order.service.OrderLifecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Distributed background scheduler that periodically sweeps unaccepted orders in {@code PLACED}
 * status whose {@code restaurantAcceptanceDeadline} has elapsed, marks them {@code CANCELLED}
 * with reason {@code RESTAURANT_UNRESPONSIVE}, and issues refunds for captured online payments.
 *
 * <p>Concurrency Safeguards:
 * <ul>
 *   <li><b>PostgreSQL {@code FOR UPDATE SKIP LOCKED}:</b> Concurrent application instances safely
 *       claim non-overlapping batches of unaccepted orders without blocking.</li>
 *   <li><b>Chunked Isolated Transactions:</b> Processed in {@code REQUIRES_NEW} transaction chunks
 *       releasing row locks immediately upon batch completion.</li>
 * </ul>
 */
@Component
public class RestaurantAcceptanceTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(RestaurantAcceptanceTimeoutScheduler.class);
    private static final int MAX_BATCHES_PER_RUN = 10;
    private static final int BATCH_SIZE = 50;

    private final OrderLifecycleService orderLifecycleService;

    public RestaurantAcceptanceTimeoutScheduler(
            OrderLifecycleService orderLifecycleService) {
        this.orderLifecycleService = orderLifecycleService;
    }

    @Scheduled(fixedDelayString = "${quickbite.order.acceptance-poll-interval-ms:30000}")
    public void sweepUnacceptedOrders() {
        Instant now = Instant.now();
        int totalProcessed = 0;

        try {
            for (int batch = 0; batch < MAX_BATCHES_PER_RUN; batch++) {
                int processedInBatch = orderLifecycleService.processRestaurantAcceptanceTimeoutBatch(now, BATCH_SIZE);
                totalProcessed += processedInBatch;

                if (processedInBatch < BATCH_SIZE) {
                    break;
                }
            }

            if (totalProcessed > 0) {
                log.info("Restaurant acceptance timeout sweep completed. Cancelled {} unaccepted orders.", totalProcessed);
            }
        } catch (Exception e) {
            log.error("Error during restaurant acceptance timeout sweep: {}", e.getMessage(), e);
        }
    }
}
