package com.quickbite.quickbite.notification.service.strategy;

import com.quickbite.quickbite.notification.dto.NotificationPayload;

/**
 * Strategy interface for notification delivery.
 *
 * Each implementation handles one delivery channel (in-app, email, push).
 * All implementations are Spring beans collected by {@link com.quickbite.quickbite.notification.service.NotificationServiceImpl}
 * via constructor injection of {@code List<NotificationDeliveryStrategy>}.
 *
 * To add a new channel (e.g. SMS):
 *   1. Add SMS to {@link NotificationChannel}.
 *   2. Create SmSNotificationStrategy implementing this interface.
 *   3. Annotate it with {@code @Component}.
 *   4. Spring auto-discovers it — no changes needed anywhere else.
 */
public interface NotificationDeliveryStrategy {

    /**
     * Deliver the notification through this strategy's channel.
     * Implementations must be idempotent where possible and must NOT throw
     * unchecked exceptions — failures are caught and logged by the service.
     */
    void deliver(NotificationPayload payload);

    /** Identifies which channel this strategy handles. */
    NotificationChannel channel();
}
