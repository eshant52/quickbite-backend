package com.quickbite.quickbite.notification.service.strategy;

import com.quickbite.quickbite.notification.dto.NotificationPayload;
import com.quickbite.quickbite.notification.repository.NotificationRepository;
import org.springframework.stereotype.Component;

/**
 * Delivers a notification by persisting it to the database for in-app display.
 *
 * Intentionally domain-agnostic — it simply calls {@code payload.toNotification()}
 * and saves the result. The specific entity type (RestaurantApplicationNotification,
 * OrderNotification, etc.) is determined entirely by the payload implementation.
 * Adding a new notification type requires zero changes here.
 */
@Component
public class InAppNotificationStrategy implements NotificationDeliveryStrategy {

    private final NotificationRepository notificationRepository;

    public InAppNotificationStrategy(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void deliver(NotificationPayload payload) {
        notificationRepository.save(payload.toNotification());
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }
}
