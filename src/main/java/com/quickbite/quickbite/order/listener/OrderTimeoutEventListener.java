package com.quickbite.quickbite.order.listener;

import com.quickbite.quickbite.common.config.property.OrderProperties;
import com.quickbite.quickbite.common.event.order.OrderPlacedEvent;
import com.quickbite.quickbite.order.service.OrderTimeoutQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener that schedules delayed timeout events into Redisson when orders
 * enter the {@code PLACED} state (both COD checkout and online payment confirmation).
 */
@Component
public class OrderTimeoutEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutEventListener.class);

    private final OrderTimeoutQueueService orderTimeoutQueueService;
    private final OrderProperties orderProperties;

    public OrderTimeoutEventListener(
            OrderTimeoutQueueService orderTimeoutQueueService,
            OrderProperties orderProperties) {
        this.orderTimeoutQueueService = orderTimeoutQueueService;
        this.orderProperties = orderProperties;
    }

    /**
     * When an order transitions to PLACED and commits, schedules the restaurant
     * acceptance deadline timeout in Redis delayed queue.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.debug("OrderPlacedEvent received for order {}. Scheduling restaurant acceptance timeout.", event.orderId());
        orderTimeoutQueueService.scheduleRestaurantAcceptanceTimeout(
                event.orderId(),
                orderProperties.restaurantAcceptanceWindow()
        );
    }
}
