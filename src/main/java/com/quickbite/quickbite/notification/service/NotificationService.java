package com.quickbite.quickbite.notification.service;

import com.quickbite.quickbite.notification.dto.NotificationPayload;

public interface NotificationService {

    /**
     * Fan-out the notification payload to every registered
     * {@link com.quickbite.quickbite.notification.service.strategy.NotificationDeliveryStrategy}.
     * Failures in individual strategies are caught and logged — one failing channel
     * (e.g. push) does not prevent delivery via other channels (e.g. in-app).
     */
    void send(NotificationPayload payload);
}
