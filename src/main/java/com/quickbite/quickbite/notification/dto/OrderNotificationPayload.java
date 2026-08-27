package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.Notification;
import com.quickbite.quickbite.notification.model.OrderNotification;
import com.quickbite.quickbite.notification.model.OrderNotificationType;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.user.model.User;

public record OrderNotificationPayload(
        User recipient,
        String title,
        String message,
        OrderNotificationType type,
        Order order
) implements NotificationPayload {

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
