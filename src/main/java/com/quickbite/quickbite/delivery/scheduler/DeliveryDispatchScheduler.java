package com.quickbite.quickbite.delivery.scheduler;

import com.quickbite.quickbite.delivery.repository.OrderDispatchRepository;
import com.quickbite.quickbite.delivery.service.DeliveryDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Background scheduler that periodically inspects active order dispatches whose next attempt
 * or round expiration timestamp has elapsed and advances dispatch progression.
 */
@Component
public class DeliveryDispatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeliveryDispatchScheduler.class);
    private static final int BATCH_SIZE = 50;

    private final OrderDispatchRepository orderDispatchRepository;
    private final DeliveryDispatchService deliveryDispatchService;

    public DeliveryDispatchScheduler(
            OrderDispatchRepository orderDispatchRepository,
            DeliveryDispatchService deliveryDispatchService) {
        this.orderDispatchRepository = orderDispatchRepository;
        this.deliveryDispatchService = deliveryDispatchService;
    }

    @Scheduled(fixedDelayString = "${quickbite.dispatch.scheduler-poll-interval-ms:5000}")
    @Transactional(readOnly = true)
    public void sweepDueDispatches() {
        Instant now = Instant.now();
        List<UUID> dueOrderIds = orderDispatchRepository.findDueDispatches(now, BATCH_SIZE);

        if (dueOrderIds.isEmpty()) {
            return;
        }

        log.debug("Processing {} due dispatches", dueOrderIds.size());
        for (UUID orderId : dueOrderIds) {
            try {
                deliveryDispatchService.processDueDispatch(orderId);
            } catch (Exception e) {
                log.error("Error processing due dispatch for order {}: {}", orderId, e.getMessage(), e);
            }
        }
    }
}
