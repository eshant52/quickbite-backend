package com.quickbite.quickbite.notification.service;

import com.quickbite.quickbite.notification.dto.NotificationPayload;
import com.quickbite.quickbite.notification.service.strategy.NotificationDeliveryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates notification delivery by fanning out the payload to ALL registered
 * {@link NotificationDeliveryStrategy} implementations.
 * <p>
 * Spring automatically collects every {@code @Component} that implements
 * {@code NotificationDeliveryStrategy} into the injected {@code List}. To add a
 * new channel (SMS, WhatsApp, …), just create a new {@code @Component} — this
 * class requires zero changes (Open/Closed Principle).
 * <p>
 * Current strategies (auto-collected):
 * - {@link com.quickbite.quickbite.notification.service.strategy.InAppNotificationStrategy}  ← saves to DB
 * - {@link com.quickbite.quickbite.notification.service.strategy.EmailNotificationStrategy}  ← stub / log only
 * - {@link com.quickbite.quickbite.notification.service.strategy.PushNotificationStrategy}   ← stub / log only
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final List<NotificationDeliveryStrategy> strategies;

    public NotificationServiceImpl(List<NotificationDeliveryStrategy> strategies) {
        this.strategies = strategies;
    }

    @Override
    public void send(NotificationPayload payload) {
        strategies.forEach(strategy -> {
            try {
                strategy.deliver(payload);
            } catch (Exception ex) {
                // A failure in one channel must never block the others.
                log.error("[NOTIFICATION] Delivery failed via channel={} recipient={} title='{}': {}",
                        strategy.channel(),
                        payload.recipient().getId(),
                        payload.title(),
                        ex.getMessage(),
                        ex);
            }
        });
    }
}
