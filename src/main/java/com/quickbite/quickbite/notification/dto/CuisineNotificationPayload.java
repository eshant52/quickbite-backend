package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.CuisineNotification;
import com.quickbite.quickbite.notification.model.CuisineNotificationType;
import com.quickbite.quickbite.notification.service.strategy.NotificationChannel;
import com.quickbite.quickbite.user.model.User;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public record CuisineNotificationPayload(
        User recipient,
        String title,
        String message,
        UUID cuisineRequestId,
        String cuisineName,
        CuisineNotificationType type,
        Set<NotificationChannel> targetChannels
) implements NotificationPayload {

    public CuisineNotificationPayload(
            User recipient,
            String title,
            String message,
            UUID cuisineRequestId,
            String cuisineName,
            CuisineNotificationType type) {
        this(recipient, title, message, cuisineRequestId, cuisineName, type,
                type == CuisineNotificationType.PENDING
                        ? Set.of(NotificationChannel.IN_APP)
                        : EnumSet.allOf(NotificationChannel.class));
    }

    @Override
    public CuisineNotification toNotification() {
        CuisineNotification notification = new CuisineNotification();
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setCuisineRequestId(cuisineRequestId);
        notification.setCuisineName(cuisineName);
        notification.setType(type);
        return notification;
    }
}
