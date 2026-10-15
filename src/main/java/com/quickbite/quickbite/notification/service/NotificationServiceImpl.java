package com.quickbite.quickbite.notification.service;

import com.quickbite.quickbite.notification.dto.NotificationPayload;
import com.quickbite.quickbite.notification.service.strategy.NotificationChannel;
import com.quickbite.quickbite.notification.service.strategy.NotificationDeliveryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orchestrates notification delivery using the Strategy + Registry Dispatcher pattern.
 * <p>
 * Spring automatically collects every {@code @Component} implementing
 * {@link NotificationDeliveryStrategy}. Strategies are registered in an immutable
 * channel lookup map for O(1) channel dispatch.
 * <p>
 * Supports:
 * - Default delivery to payload's target channels (or broadcast)
 * - Explicit single-channel delivery (e.g. {@code sendToChannel(payload, NotificationChannel.EMAIL)})
 * - Targeted multi-channel delivery (e.g. {@code sendToChannels(payload, Set.of(...))})
 * <p>
 * Current strategies (auto-collected):
 * - {@link com.quickbite.quickbite.notification.service.strategy.InAppNotificationStrategy}  ← saves to DB
 * - {@link com.quickbite.quickbite.notification.service.strategy.EmailNotificationStrategy}  ← stub / log only
 * - {@link com.quickbite.quickbite.notification.service.strategy.PushNotificationStrategy}   ← stub / log only
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final Map<NotificationChannel, List<NotificationDeliveryStrategy>> strategyRegistry;

    public NotificationServiceImpl(List<NotificationDeliveryStrategy> strategies) {
        this.strategyRegistry = strategies.stream()
                .collect(Collectors.groupingBy(
                        NotificationDeliveryStrategy::channel,
                        Collectors.toUnmodifiableList()
                ));
    }

    @Override
    public void send(NotificationPayload payload) {
        if (payload == null) {
            log.warn("[NOTIFICATION] Attempted to send null notification payload");
            return;
        }
        sendToChannels(payload, payload.targetChannels());
    }

    @Override
    public void sendToChannel(NotificationPayload payload, NotificationChannel channel) {
        if (payload == null) {
            log.warn("[NOTIFICATION] Attempted to send null notification payload to channel={}", channel);
            return;
        }
        if (channel == null) {
            log.warn("[NOTIFICATION] Attempted to send notification with null channel for recipient={}",
                    payload.recipient() != null ? payload.recipient().getId() : "null");
            return;
        }

        List<NotificationDeliveryStrategy> strategies = strategyRegistry.get(channel);
        if (strategies == null || strategies.isEmpty()) {
            log.warn("[NOTIFICATION] No strategy registered for channel={}", channel);
            return;
        }

        strategies.forEach(strategy -> deliverSafely(strategy, payload));
    }

    @Override
    public void sendToChannels(NotificationPayload payload, Set<NotificationChannel> channels) {
        if (payload == null) {
            log.warn("[NOTIFICATION] Attempted to send null notification payload to channels={}", channels);
            return;
        }
        if (channels == null || channels.isEmpty()) {
            log.debug("[NOTIFICATION] No target channels specified for recipient={} title='{}'",
                    payload.recipient() != null ? payload.recipient().getId() : "null", payload.title());
            return;
        }

        channels.forEach(channel -> sendToChannel(payload, channel));
    }

    private void deliverSafely(NotificationDeliveryStrategy strategy, NotificationPayload payload) {
        try {
            strategy.deliver(payload);
        } catch (Exception ex) {
            // A failure in one channel must never block the others.
            log.error("[NOTIFICATION] Delivery failed via channel={} recipient={} title='{}': {}",
                    strategy.channel(),
                    payload.recipient() != null ? payload.recipient().getId() : "null",
                    payload.title(),
                    ex.getMessage(),
                    ex);
        }
    }
}
