package com.quickbite.quickbite.notification.listener;

import com.quickbite.quickbite.common.event.QuickBiteTopics;
import com.quickbite.quickbite.common.event.order.OrderCancelledEvent;
import com.quickbite.quickbite.common.event.order.OrderEvent;
import com.quickbite.quickbite.common.event.order.OrderPlacedEvent;
import com.quickbite.quickbite.common.event.order.OrderStatusChangedEvent;
import com.quickbite.quickbite.notification.dto.OrderNotificationPayload;
import com.quickbite.quickbite.notification.model.OrderNotificationType;
import com.quickbite.quickbite.notification.service.NotificationService;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public OrderEventListener(NotificationService notificationService,
                              UserRepository userRepository,
                              OrderRepository orderRepository,
                              ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = QuickBiteTopics.ORDER_EVENTS,
            groupId = "quickbite-notification-group",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void onOrderEvent(String rawEvent) {
        OrderEvent event = deserialize(rawEvent, OrderEvent.class);
        if (event == null) return;

        switch (event) {
            case OrderPlacedEvent placed -> handleOrderPlaced(placed);
            case OrderStatusChangedEvent statusChanged -> handleOrderStatusChanged(statusChanged);
            case OrderCancelledEvent cancelled -> handleOrderCancelled(cancelled);
        }
    }

    private void handleOrderPlaced(OrderPlacedEvent event) {
        User customer = loadCustomer(event.customerId());
        if (customer == null) {
            log.error("[NOTIFICATION] Customer not found for order: {}", event.orderId());
            return;
        }

        Order order = loadOrderOfCustomer(event.orderId(), customer);
        if (order == null) {
            log.error("[NOTIFICATION] Order not found for order: {}", event.orderId());
            return;
        }

        notificationService.send(new OrderNotificationPayload(
                customer,
                "Order Confirmed!",
                "Your order of amount ₹" + event.totalAmount() + " has been placed successfully.",
                OrderNotificationType.PLACED,
                order
        ));

        log.info("[NOTIFICATION] Order placed event sent to order: {}", event.orderId());
    }

    private void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        User customer = loadCustomer(event.customerId());
        if (customer == null) {
            log.error("[NOTIFICATION] Customer not found for order: {}", event.orderId());
            return;
        }

        Order order = loadOrderOfCustomer(event.orderId(), customer);
        if (order == null) {
            log.error("[NOTIFICATION] Order not found for order: {}", event.orderId());
            return;
        }

        Map<String, String> titleAndMessage = getTitleAndMessage(event.newStatus());

        OrderNotificationType notificationType = switch (event.newStatus()) {
            case ACCEPTED -> OrderNotificationType.ACCEPTED;
            case PREPARING -> OrderNotificationType.PREPARING;
            case READY_FOR_PICKUP -> OrderNotificationType.READY_FOR_PICKUP;
            case OUT_FOR_DELIVERY -> OrderNotificationType.OUT_FOR_DELIVERY;
            case DELIVERED -> OrderNotificationType.DELIVERED;
            case DECLINED -> OrderNotificationType.DECLINED;
            case PAYMENT_FAILED -> OrderNotificationType.PAYMENT_FAILED;
            default -> null;
        };

        if (notificationType == null) return;

        notificationService.send(new OrderNotificationPayload(
                customer,
                titleAndMessage.get("title"),
                titleAndMessage.get("message"),
                notificationType,
                order
        ));

        log.info("[NOTIFICATION] Order {} updated for order: {}", event.newStatus().name(), event.orderId());
    }

    private void handleOrderCancelled(OrderCancelledEvent event) {
        User customer = loadCustomer(event.customerId());
        if (customer == null) {
            log.error("[NOTIFICATION] Customer not found for order: {}", event.orderId());
            return;
        }

        Order order = loadOrderOfCustomer(event.orderId(), customer);
        if (order == null) {
            log.error("[NOTIFICATION] Order not found for order: {}", event.orderId());
            return;
        }

        notificationService.send(new OrderNotificationPayload(
                customer,
                "Order Cancelled!",
                "Your order has been cancelled. If you have any questions, please contact support.",
                OrderNotificationType.CANCELLED,
                order
        ));

        log.info("[NOTIFICATION] Order cancelled for order: {}", event.orderId());
    }

    private <T> T deserialize(String rawEvent, Class<T> type) {
        try {
            return objectMapper.readValue(rawEvent, type);
        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed to deserialize Kafka event: {}", rawEvent, e);
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }

    private User loadCustomer(UUID customerId) {
        return userRepository.findById(customerId).orElse(null);
    }

    private Order loadOrderOfCustomer(UUID orderId, User customer) {
        return orderRepository.findByIdAndCustomerId(orderId, customer.getId()).orElse(null);
    }

    private Map<String, String> getTitleAndMessage(OrderStatus newStatus) {
        return switch (newStatus) {
            case ACCEPTED -> Map.of(
                    "title", "Restaurant Accepted Your Order!",
                    "message", "Your order has been accepted by the restaurant."
            );
            case PREPARING -> Map.of(
                    "title", "Food is Being Prepared!",
                    "message", "Your order is currently being prepared."
            );
            case READY_FOR_PICKUP -> Map.of(
                    "title", "Order Ready - Assigning Delivery Partner",
                    "message", "Your order is ready for pickup."
            );
            case OUT_FOR_DELIVERY -> Map.of(
                    "title", "Order is On the Way!",
                    "message", "Your order is out for delivery."
            );
            case DELIVERED -> Map.of(
                    "title", "Order Delivered!",
                    "message", "Your order has been delivered. Enjoy your meal!"
            );
            case DECLINED -> Map.of(
                    "title", "Order Declined",
                    "message", "Your order has been declined."
            );
            case PAYMENT_FAILED -> Map.of(
                    "title", "Payment Failed",
                    "message", "Payment for your order has failed."
            );
            default -> Map.of(
                    "title", "Order Updated",
                    "message", "Your order status has been updated."
            );
        };
    }
}
