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
        Order order = findOrder(event.orderId(), event.customerId());
        if (order == null) {
            log.error("[NOTIFICATION] Order not found for placed event: {}", event.orderId());
            return;
        }

        notifyOrderCustomer(
                order,
                "Order Confirmed!",
                "Your order of amount ₹" + event.totalAmount() + " has been placed successfully.",
                OrderNotificationType.PLACED,
                "Order placed event sent"
        );

        notifyRestaurantOwner(
                order,
                "New Order Placed!",
                "New order #" + order.getId() + " received for ₹" + event.totalAmount() + ". Please review and accept.",
                OrderNotificationType.PLACED
        );
    }

    private void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        Order order = findOrder(event.orderId(), event.customerId());
        if (order == null) {
            log.error("[NOTIFICATION] Order not found for status change event: {}", event.orderId());
            return;
        }

        StatusNotificationConfig config = resolveStatusConfig(event.newStatus());
        if (config != null) {
            notifyOrderCustomer(
                    order,
                    config.title(),
                    config.message(),
                    config.type(),
                    "Order " + event.newStatus().name() + " updated"
            );
        }

        switch (event.newStatus()) {
            case READY_FOR_PICKUP -> notifyDeliveryAgent(
                    order,
                    "Order Ready for Pickup!",
                    "Order #" + order.getId() + " is ready for pickup at " + order.getRestaurant().getName() + ".",
                    OrderNotificationType.READY_FOR_PICKUP
            );
            case OUT_FOR_DELIVERY -> notifyRestaurantOwner(
                    order,
                    "Order Picked Up",
                    "Order #" + order.getId() + " has been picked up by the delivery partner and is out for delivery.",
                    OrderNotificationType.OUT_FOR_DELIVERY
            );
            case DELIVERED -> {
                notifyRestaurantOwner(
                        order,
                        "Order Delivered!",
                        "Order #" + order.getId() + " was successfully delivered to the customer.",
                        OrderNotificationType.DELIVERED
                );
                notifyDeliveryAgent(
                        order,
                        "Delivery Complete!",
                        "You have successfully delivered order #" + order.getId() + ".",
                        OrderNotificationType.DELIVERED
                );
            }
            default -> {}
        }
    }

    private void handleOrderCancelled(OrderCancelledEvent event) {
        Order order = findOrder(event.orderId(), event.customerId());
        if (order == null) {
            log.error("[NOTIFICATION] Order not found for cancelled event: {}", event.orderId());
            return;
        }

        notifyOrderCustomer(
                order,
                "Order Cancelled!",
                "Your order has been cancelled. If you have any questions, please contact support.",
                OrderNotificationType.CANCELLED,
                "Order cancelled"
        );

        notifyRestaurantOwner(
                order,
                "Order Cancelled",
                "Order #" + order.getId() + " has been cancelled.",
                OrderNotificationType.CANCELLED
        );

        notifyDeliveryAgent(
                order,
                "Order Cancelled",
                "Order #" + order.getId() + " has been cancelled.",
                OrderNotificationType.CANCELLED
        );
    }

    private void notifyOrderCustomer(
            Order order,
            String title,
            String message,
            OrderNotificationType type,
            String actionLog) {
        if (order.getCustomer() == null) {
            log.error("[NOTIFICATION] Customer not found for order: {}", order.getId());
            return;
        }

        notificationService.send(new OrderNotificationPayload(
                order.getCustomer(),
                title,
                message,
                type,
                order
        ));

        log.info("[NOTIFICATION] {} for order: {}", actionLog, order.getId());
    }

    private void notifyRestaurantOwner(
            Order order,
            String title,
            String message,
            OrderNotificationType type) {
        if (order.getRestaurant() != null && order.getRestaurant().getOwner() != null) {
            notificationService.send(new OrderNotificationPayload(
                    order.getRestaurant().getOwner(),
                    title,
                    message,
                    type,
                    order
            ));
            log.info("[NOTIFICATION] Sent {} to restaurant owner for order: {}", type, order.getId());
        }
    }

    private void notifyDeliveryAgent(
            Order order,
            String title,
            String message,
            OrderNotificationType type) {
        if (order.getDeliveryAgent() != null && order.getDeliveryAgent().getUser() != null) {
            notificationService.send(new OrderNotificationPayload(
                    order.getDeliveryAgent().getUser(),
                    title,
                    message,
                    type,
                    order
            ));
            log.info("[NOTIFICATION] Sent {} to delivery agent for order: {}", type, order.getId());
        }
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

    private Order findOrder(UUID orderId, UUID customerId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null && customerId != null) {
            order = orderRepository.findByIdAndCustomerId(orderId, customerId).orElse(null);
        }
        if (order != null && order.getCustomer() == null && customerId != null) {
            userRepository.findById(customerId).ifPresent(order::setCustomer);
        }
        return order;
    }
}
