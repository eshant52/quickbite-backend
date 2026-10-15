package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.Notification;
import com.quickbite.quickbite.notification.model.OrderNotification;
import com.quickbite.quickbite.notification.model.OrderNotificationType;
import com.quickbite.quickbite.notification.service.strategy.NotificationChannel;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.user.model.User;

import java.util.EnumSet;
import java.util.Set;

public record OrderNotificationPayload(
        User recipient,
        String title,
        String message,
        OrderNotificationType type,
        Order order,
        Set<NotificationChannel> targetChannels
) implements NotificationPayload {

    public OrderNotificationPayload(
            User recipient,
            String title,
            String message,
            OrderNotificationType type,
            Order order) {
        this(recipient, title, message, type, order, resolveDefaultChannels(type));
    }

    private static Set<NotificationChannel> resolveDefaultChannels(OrderNotificationType type) {
        if (type == null) {
            return EnumSet.allOf(NotificationChannel.class);
        }
        return switch (type) {
            case ACCEPTED, PREPARING, READY_FOR_PICKUP, OUT_FOR_DELIVERY, AGENT_ASSIGNED, DELIVERY_OFFER_RECEIVED ->
                    Set.of(NotificationChannel.IN_APP, NotificationChannel.PUSH);
            case PLACED, DELIVERED, CANCELLED, DECLINED, PAYMENT_FAILED, ABANDONED, RESTAURANT_TIMEOUT, NO_AGENT_FOUND, AWAITING_PAYMENT ->
                    EnumSet.allOf(NotificationChannel.class);
        };
    }

    @Override
    public Notification toNotification() {
        OrderNotification notification = new OrderNotification();
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setType(type);
        notification.setOrder(order);
        return notification;
    }
}
