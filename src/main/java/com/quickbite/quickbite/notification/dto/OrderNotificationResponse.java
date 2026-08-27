package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.OrderNotification;
import com.quickbite.quickbite.notification.model.OrderNotificationType;

import java.time.Instant;
import java.util.UUID;

public record OrderNotificationResponse(
        UUID id,
        String title,
        String message,
        boolean isRead,
        UUID orderId,
        OrderNotificationType type,
        Instant createdAt
) implements NotificationResponse {
    public static OrderNotificationResponse from(OrderNotification orderNotification) {
        return new OrderNotificationResponse(
                orderNotification.getId(),
                orderNotification.getTitle(),
                orderNotification.getMessage(),
                orderNotification.isRead(),
                orderNotification.getOrder() != null ? orderNotification.getOrder().getId() : null,
                orderNotification.getType(),
                orderNotification.getCreatedAt()
        );
    }
}
