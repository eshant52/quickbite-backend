package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.VehicleApplicationNotification;
import com.quickbite.quickbite.notification.model.VehicleApplicationNotificationType;

import java.util.UUID;

public record VehicleApplicationNotificationResponse(
        UUID id,
        String title,
        String message,
        boolean isRead,
        VehicleApplicationNotificationType type,
        UUID applicationId,
        String vehicleName
) implements NotificationResponse {

    public static VehicleApplicationNotificationResponse from(VehicleApplicationNotification n) {
        return new VehicleApplicationNotificationResponse(
                n.getId(),
                n.getTitle(),
                n.getMessage(),
                n.isRead(),
                n.getType(),
                n.getApplicationId(),
                n.getVehicleName()
        );
    }
}
