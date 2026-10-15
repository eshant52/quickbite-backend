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

import java.util.UUID;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public OrderEventListener(NotificationService notificationService,
                              UserRepository userRepository,
                              OrderRepository orderRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @KafkaListener(
            topics = QuickBiteTopics.ORDER_EVENTS,
            groupId = "quickbite-notification-group",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void onOrderEvent(OrderEvent event) {
        if (event == null) return;

        switch (event) {
            case OrderPlacedEvent placed -> handleOrderPlaced(placed);
            case OrderStatusChangedEvent statusChanged -> handleOrderStatusChanged(statusChanged);
            case OrderCancelledEvent cancelled -> handleOrderCancelled(cancelled);
        }
    }

    private void handleOrderPlaced(OrderPlacedEvent event) {
        notifyOrderCustomer(
                event.orderId(),
                event.customerId(),
                "Order Confirmed!",
                "Your order of amount ₹" + event.totalAmount() + " has been placed successfully.",
                OrderNotificationType.PLACED,
                "Order placed event sent"
        );
    }

    private void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        StatusNotificationConfig config = resolveStatusConfig(event.newStatus());
        if (config == null) {
            return;
        }

        notifyOrderCustomer(
                event.orderId(),
                event.customerId(),
                config.title(),
                config.message(),
                config.type(),
                "Order " + event.newStatus().name() + " updated"
        );
    }

    private void handleOrderCancelled(OrderCancelledEvent event) {
        notifyOrderCustomer(
                event.orderId(),
                event.customerId(),
                "Order Cancelled!",
                "Your order has been cancelled. If you have any questions, please contact support.",
                OrderNotificationType.CANCELLED,
                "Order cancelled"
        );
    }

    private void notifyOrderCustomer(
            UUID orderId,
            UUID customerId,
            String title,
            String message,
            OrderNotificationType type,
            String actionLog) {
        User customer = userRepository.findById(customerId).orElse(null);
        if (customer == null) {
            log.error("[NOTIFICATION] Customer not found for order: {}", orderId);
            return;
        }

        Order order = orderRepository.findByIdAndCustomerId(orderId, customer.getId()).orElse(null);
        if (order == null) {
            log.error("[NOTIFICATION] Order not found for order: {}", orderId);
            return;
        }

        notificationService.send(new OrderNotificationPayload(
                customer,
                title,
                message,
                type,
                order
        ));

        log.info("[NOTIFICATION] {} for order: {}", actionLog, orderId);
    }

    private record StatusNotificationConfig(OrderNotificationType type, String title, String message) {}

    private StatusNotificationConfig resolveStatusConfig(OrderStatus newStatus) {
        return switch (newStatus) {
            case ACCEPTED -> new StatusNotificationConfig(
                    OrderNotificationType.ACCEPTED,
                    "Restaurant Accepted Your Order!",
                    "Your order has been accepted by the restaurant."
            );
            case PREPARING -> new StatusNotificationConfig(
                    OrderNotificationType.PREPARING,
                    "Food is Being Prepared!",
                    "Your order is currently being prepared."
            );
            case READY_FOR_PICKUP -> new StatusNotificationConfig(
                    OrderNotificationType.READY_FOR_PICKUP,
                    "Order Ready - Assigning Delivery Partner",
                    "Your order is ready for pickup."
            );
            case OUT_FOR_DELIVERY -> new StatusNotificationConfig(
                    OrderNotificationType.OUT_FOR_DELIVERY,
                    "Order is On the Way!",
                    "Your order is out for delivery."
            );
            case DELIVERED -> new StatusNotificationConfig(
                    OrderNotificationType.DELIVERED,
                    "Order Delivered!",
                    "Your order has been delivered. Enjoy your meal!"
            );
            case DECLINED -> new StatusNotificationConfig(
                    OrderNotificationType.DECLINED,
                    "Order Declined",
                    "Your order has been declined."
            );
            case PAYMENT_FAILED -> new StatusNotificationConfig(
                    OrderNotificationType.PAYMENT_FAILED,
                    "Payment Failed",
                    "Payment for your order has failed."
            );
            case ABANDONED -> new StatusNotificationConfig(
                    OrderNotificationType.ABANDONED,
                    "Order Cancelled due to Inactivity",
                    "Your order was cancelled because payment was not completed within the allowed time."
            );
            default -> null;
        };
    }
}
