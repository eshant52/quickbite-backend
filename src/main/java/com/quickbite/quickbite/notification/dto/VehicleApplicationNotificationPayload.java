package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.Notification;
import com.quickbite.quickbite.notification.model.VehicleApplicationNotification;
import com.quickbite.quickbite.notification.model.VehicleApplicationNotificationType;
import com.quickbite.quickbite.user.model.User;

import java.util.UUID;

/**
 * Notification payload for vehicle application lifecycle events.
 */
public record VehicleApplicationNotificationPayload(
        User recipient,
        String title,
        String message,
        VehicleApplicationNotificationType type,
        UUID applicationId,
        String vehicleName
) implements NotificationPayload {

    @Override
    public Notification toNotification() {
        VehicleApplicationNotification notification = new VehicleApplicationNotification();
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setType(type);
        notification.setApplicationId(applicationId);
        notification.setVehicleName(vehicleName);
        return notification;
    }
}
