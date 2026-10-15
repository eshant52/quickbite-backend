package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.Notification;
import com.quickbite.quickbite.notification.service.strategy.NotificationChannel;
import com.quickbite.quickbite.user.model.User;

import java.util.EnumSet;
import java.util.Set;

/**
 * Sealed interface representing everything a notification delivery strategy needs.
 */
public sealed interface NotificationPayload permits
        RestaurantApplicationNotificationPayload,
        OrderNotificationPayload,
        PaymentNotificationPayload,
        CuisineNotificationPayload,
        DeliveryAgentApplicationNotificationPayload,
        VehicleApplicationNotificationPayload {

    /**
     * The user who should receive this notification.
     */
    User recipient();

    /**
     * Short subject line — max 200 chars (matches the DB column length).
     */
    String title();

    /**
     * Full body text — max 1000 chars.
     */
    String message();

    /**
     * Factory method: constructs the correct {@link Notification} subtype populated
     * with all domain-specific fields.
     */
    Notification toNotification();

    /**
     * Set of delivery channels this payload targets by default.
     * Defaults to all channels. Implementations may override this.
     */
    default Set<NotificationChannel> targetChannels() {
        return EnumSet.allOf(NotificationChannel.class);
    }
}
