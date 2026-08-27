package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.Notification;
import com.quickbite.quickbite.notification.model.PaymentNotification;
import com.quickbite.quickbite.notification.model.PaymentNotificationType;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.user.model.User;

public record PaymentNotificationPayload(
        User recipient,
        String title,
        String message,
        PaymentNotificationType type,
        Payment payment
) implements NotificationPayload {

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
