package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.DeliveryAgentApplicationNotification;
import com.quickbite.quickbite.notification.model.DeliveryAgentApplicationNotificationType;
import com.quickbite.quickbite.notification.model.Notification;
import com.quickbite.quickbite.notification.service.strategy.NotificationChannel;
import com.quickbite.quickbite.user.model.User;

import java.util.EnumSet;
import java.util.Set;
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
        String agentName,
        Set<NotificationChannel> targetChannels
) implements NotificationPayload {

    public DeliveryAgentApplicationNotificationPayload(
            User recipient,
            String title,
            String message,
            DeliveryAgentApplicationNotificationType type,
            UUID applicationId,
            String agentName) {
        this(recipient, title, message, type, applicationId, agentName,
                type == DeliveryAgentApplicationNotificationType.APPLICATION_SUBMITTED
                        ? Set.of(NotificationChannel.IN_APP)
                        : EnumSet.allOf(NotificationChannel.class));
    }

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
