package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.Notification;
import com.quickbite.quickbite.notification.model.PaymentNotification;
import com.quickbite.quickbite.notification.model.PaymentNotificationType;
import com.quickbite.quickbite.notification.service.strategy.NotificationChannel;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.user.model.User;

import java.util.EnumSet;
import java.util.Set;

public record PaymentNotificationPayload(
        User recipient,
        String title,
        String message,
        PaymentNotificationType type,
        Payment payment,
        Set<NotificationChannel> targetChannels
) implements NotificationPayload {

    public PaymentNotificationPayload(
            User recipient,
            String title,
            String message,
            PaymentNotificationType type,
            Payment payment) {
        this(recipient, title, message, type, payment, resolveDefaultChannels(type));
    }

    private static Set<NotificationChannel> resolveDefaultChannels(PaymentNotificationType type) {
        if (type == null) {
            return EnumSet.allOf(NotificationChannel.class);
        }
        return switch (type) {
            case SUCCESS ->
                    Set.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
            case FAILED, CANCELLED ->
                    Set.of(NotificationChannel.IN_APP, NotificationChannel.PUSH);
            case REFUNDED, REFUND_FAILED ->
                    EnumSet.allOf(NotificationChannel.class);
            case PENDING ->
                    Set.of(NotificationChannel.IN_APP);
        };
    }

    @Override
    public Notification toNotification() {
        PaymentNotification notification = new PaymentNotification();
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setPayment(payment);
        return notification;
    }
}
