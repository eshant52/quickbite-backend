package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.Notification;
import com.quickbite.quickbite.notification.model.RestaurantApplicationNotification;
import com.quickbite.quickbite.notification.model.RestaurantApplicationNotificationType;
import com.quickbite.quickbite.user.model.User;

import java.util.UUID;

/**
 * Notification payload for restaurant onboarding lifecycle events.
 * <p>
 * Implements {@link NotificationPayload} and owns the creation of a
 * {@link RestaurantApplicationNotification} entity via {@link #toNotification()}.
 *
 * <p>Used by all three delivery strategies:
 * <ul>
 *   <li><b>InApp</b> — calls {@link #toNotification()} and persists the result.</li>
 *   <li><b>Email</b> — uses only {@link #recipient()}, {@link #title()}, {@link #message()}.</li>
 *   <li><b>Push</b>  — uses only {@link #recipient()}, {@link #title()}, {@link #message()}.</li>
 * </ul>
 *
 * @param recipient      The user to notify (an admin or the restaurant owner).
 * @param title          Short subject line.
 * @param message        Full notification body.
 * @param type           Specific event type within restaurant application lifecycle.
 * @param applicationId  ID of the source RestaurantApplication (no FK — historical record).
 * @param restaurantName Snapshot of the restaurant name at event time.
 */
public record RestaurantApplicationNotificationPayload(
        User recipient,
        String title,
        String message,
        RestaurantApplicationNotificationType type,
        UUID applicationId,
        String restaurantName
) implements NotificationPayload {

    @Override
    public Notification toNotification() {
        RestaurantApplicationNotification notification = new RestaurantApplicationNotification();
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setType(type);
        notification.setApplicationId(applicationId);
        notification.setRestaurantName(restaurantName);
        return notification;
    }
}
