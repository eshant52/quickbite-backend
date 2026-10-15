package com.quickbite.quickbite.notification.service;

import com.quickbite.quickbite.notification.dto.NotificationPayload;
import com.quickbite.quickbite.notification.service.strategy.NotificationChannel;

import java.util.Set;

public interface NotificationService {

    /**
     * Delivers the notification payload to its configured target channels (by default,
     * all registered channels).
     * Failures in individual strategies are caught and logged — one failing channel
     * does not prevent delivery via other channels.
     *
     * @param payload the notification payload
     */
    void send(NotificationPayload payload);

    /**
     * Delivers the notification exclusively to the specified delivery channel.
     *
     * @param payload the notification payload
     * @param channel the target delivery channel
     */
    void sendToChannel(NotificationPayload payload, NotificationChannel channel);

    /**
     * Delivers the notification to the specified set of delivery channels.
     *
     * @param payload  the notification payload
     * @param channels the target delivery channels
     */
    void sendToChannels(NotificationPayload payload, Set<NotificationChannel> channels);
}
