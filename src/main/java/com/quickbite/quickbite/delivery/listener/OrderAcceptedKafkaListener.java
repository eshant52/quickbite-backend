package com.quickbite.quickbite.delivery.listener;

import com.quickbite.quickbite.common.event.QuickBiteTopics;
import com.quickbite.quickbite.common.event.order.OrderEvent;
import com.quickbite.quickbite.common.event.order.OrderStatusChangedEvent;
import com.quickbite.quickbite.delivery.service.DeliveryDispatchService;
import com.quickbite.quickbite.order.model.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderAcceptedKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(OrderAcceptedKafkaListener.class);

    private final DeliveryDispatchService deliveryDispatchService;

    public OrderAcceptedKafkaListener(DeliveryDispatchService deliveryDispatchService) {
        this.deliveryDispatchService = deliveryDispatchService;
    }

    @KafkaListener(
            topics = QuickBiteTopics.ORDER_EVENTS,
            groupId = "quickbite-delivery-dispatch-group",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void onOrderEvent(OrderEvent event) {
        if (event instanceof OrderStatusChangedEvent statusChanged
                && statusChanged.newStatus() == OrderStatus.ACCEPTED) {
            log.info("[Kafka] OrderStatusChangedEvent (ACCEPTED) received for order={}. Initiating delivery dispatch.",
                    statusChanged.orderId());
            try {
                deliveryDispatchService.initiateDispatch(statusChanged.orderId());
            } catch (Exception e) {
                log.error("[Kafka] Failed to initiate dispatch for order={}: {}",
                        statusChanged.orderId(), e.getMessage(), e);
            }
        }
    }
}
