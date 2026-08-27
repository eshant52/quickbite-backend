package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.PaymentNotification;
import com.quickbite.quickbite.notification.model.PaymentNotificationType;

import java.time.Instant;
import java.util.UUID;

public record PaymentNotificationResponse(
        UUID id,
        String title,
        String message,
        boolean isRead,
        UUID paymentId,
        PaymentNotificationType type,
        Instant createdAt
) implements NotificationResponse {
    public static PaymentNotificationResponse from(PaymentNotification paymentNotification) {
        return new PaymentNotificationResponse(
                paymentNotification.getId(),
                paymentNotification.getTitle(),
                paymentNotification.getMessage(),
                paymentNotification.isRead(),
                paymentNotification.getPayment() != null ? paymentNotification.getPayment().getId() : null,
                paymentNotification.getType(),
                paymentNotification.getCreatedAt()
        );
    }
}
