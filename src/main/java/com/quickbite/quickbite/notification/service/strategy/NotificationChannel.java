package com.quickbite.quickbite.notification.service.strategy;

/**
 * Represents the delivery channel through which a notification can be sent.
 *
 * Current state:
 * - IN_APP  → implemented via {@link InAppNotificationStrategy}
 * - EMAIL   → stub in {@link EmailNotificationStrategy}, ready for wiring
 * - PUSH    → stub in {@link PushNotificationStrategy}, ready for wiring
 */
public enum NotificationChannel {
    IN_APP,
    EMAIL,
    PUSH
}
