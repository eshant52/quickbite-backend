package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.DeliveryAgentApplicationNotification;
import com.quickbite.quickbite.notification.model.DeliveryAgentApplicationNotificationType;

import java.util.UUID;

public record DeliveryAgentApplicationNotificationResponse(
        UUID id,
        String title,
        String message,
        boolean isRead,
        DeliveryAgentApplicationNotificationType type,
        UUID applicationId,
        String agentName
) implements NotificationResponse {

    public static DeliveryAgentApplicationNotificationResponse from(DeliveryAgentApplicationNotification n) {
        return new DeliveryAgentApplicationNotificationResponse(
                n.getId(),
                n.getTitle(),
                n.getMessage(),
                n.isRead(),
                n.getType(),
                n.getApplicationId(),
                n.getAgentName()
        );
    }
}
