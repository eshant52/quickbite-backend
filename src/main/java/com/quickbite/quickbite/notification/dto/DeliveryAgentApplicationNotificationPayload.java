package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.DeliveryAgentApplicationNotification;
import com.quickbite.quickbite.notification.model.DeliveryAgentApplicationNotificationType;
import com.quickbite.quickbite.notification.model.Notification;
import com.quickbite.quickbite.user.model.User;

import java.util.UUID;

/**
 * Notification payload for delivery agent onboarding lifecycle events.
 */
public record DeliveryAgentApplicationNotificationPayload(
        User recipient,
        String title,
        String message,
        DeliveryAgentApplicationNotificationType type,
        UUID applicationId,
        String agentName
) implements NotificationPayload {

    @Override
    public Notification toNotification() {
        DeliveryAgentApplicationNotification notification = new DeliveryAgentApplicationNotification();
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setType(type);
        notification.setApplicationId(applicationId);
        notification.setAgentName(agentName);
        return notification;
    }
}
