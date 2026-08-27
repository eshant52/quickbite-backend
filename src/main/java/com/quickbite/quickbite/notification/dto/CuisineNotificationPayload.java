package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.CuisineNotification;
import com.quickbite.quickbite.notification.model.CuisineNotificationType;
import com.quickbite.quickbite.user.model.User;

import java.util.UUID;

public record CuisineNotificationPayload(
        User recipient,
        String title,
        String message,
        UUID cuisineRequestId,
        String cuisineName,
        CuisineNotificationType type
) implements NotificationPayload {

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
