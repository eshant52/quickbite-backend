package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.RestaurantApplicationNotification;
import com.quickbite.quickbite.notification.model.RestaurantApplicationNotificationType;

import java.time.Instant;
import java.util.UUID;

public record RestaurantApplicationNotificationResponse(
        UUID id,
        String title,
        String message,
        boolean isRead,
        UUID applicationId,
        RestaurantApplicationNotificationType type,
        String restaurantName,
        Instant createdAt
) implements NotificationResponse {
    public static RestaurantApplicationNotificationResponse from(RestaurantApplicationNotification notification) {
        return new RestaurantApplicationNotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getApplicationId(),
                notification.getType(),
                notification.getRestaurantName(),
                notification.getCreatedAt()
        );
    }
}
