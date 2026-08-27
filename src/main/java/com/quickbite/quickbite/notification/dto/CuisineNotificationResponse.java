package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.CuisineNotification;
import com.quickbite.quickbite.notification.model.CuisineNotificationType;

import java.time.Instant;
import java.util.UUID;

public record CuisineNotificationResponse(
        UUID id,
        String title,
        String message,
        boolean isRead,
        UUID cuisineRequestId,
        String cuisineName,
        CuisineNotificationType type,
        Instant createdAt
) implements NotificationResponse {

    public static CuisineNotificationResponse from(CuisineNotification notification) {
        return new CuisineNotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCuisineRequestId(),
                notification.getCuisineName(),
                notification.getType(),
                notification.getCreatedAt()
        );
    }
}
