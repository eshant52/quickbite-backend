package com.quickbite.quickbite.notification.listener;

import com.quickbite.quickbite.common.event.QuickBiteTopics;
import com.quickbite.quickbite.common.event.delivery.DeliveryAgentAssignedEvent;
import com.quickbite.quickbite.common.event.delivery.DeliveryEvent;
import com.quickbite.quickbite.common.event.delivery.DeliveryOfferCreatedEvent;
import com.quickbite.quickbite.notification.dto.OrderNotificationPayload;
import com.quickbite.quickbite.notification.model.OrderNotificationType;
import com.quickbite.quickbite.notification.service.NotificationService;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
public class DeliveryEventListener {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventListener.class);

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public DeliveryEventListener(
            NotificationService notificationService,
            UserRepository userRepository,
            OrderRepository orderRepository,
            ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = QuickBiteTopics.DELIVERY_EVENTS,
            groupId = "quickbite-delivery-notification-group",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void onDeliveryEvent(String rawEvent) {
        DeliveryEvent event = deserialize(rawEvent, DeliveryEvent.class);
        if (event == null) return;

        switch (event) {
            case DeliveryAgentAssignedEvent assigned -> handleAgentAssigned(assigned);
            case DeliveryOfferCreatedEvent offer -> handleOfferCreated(offer);
            default -> log.debug("Unhandled delivery event: {}", event);
        }
    }

    private void handleAgentAssigned(DeliveryAgentAssignedEvent event) {
        User customer = userRepository.findById(event.customerId()).orElse(null);
        if (customer == null) {
            log.error("[NOTIFICATION] Customer {} not found for assigned delivery", event.customerId());
            return;
        }

        Order order = orderRepository.findById(event.orderId()).orElse(null);
        notificationService.send(new OrderNotificationPayload(
                customer,
                "Delivery Partner Assigned!",
                "Your delivery partner " + event.agentName() + " has been assigned to your order.",
                OrderNotificationType.AGENT_ASSIGNED,
                order
        ));
    }

    private void handleOfferCreated(DeliveryOfferCreatedEvent event) {
        // TODO(issue #N): Implement FCM push notification to agent when offer is created
        log.info("[NOTIFICATION] Push notification: New delivery offer {} for agent {}", event.offerId(), event.agentId());
    }

    private <T> T deserialize(String rawEvent, Class<T> type) {
        try {
            return objectMapper.readValue(rawEvent, type);
        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed to deserialize DeliveryEvent: {}", rawEvent, e);
            return null;
        }
    }
}
