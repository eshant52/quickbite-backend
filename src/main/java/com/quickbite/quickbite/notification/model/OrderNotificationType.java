package com.quickbite.quickbite.notification.model;

public enum OrderNotificationType {
    // Initial status
    AWAITING_PAYMENT,

    // Intermediate statuses
    PLACED,
    ACCEPTED,
    RESTAURANT_TIMEOUT,
    PREPARING,
    READY_FOR_PICKUP,
    OUT_FOR_DELIVERY,

    // Terminal statuses
    PAYMENT_FAILED,
    CANCELLED,
    DECLINED,
    DELIVERED,
    ABANDONED,

    // Delivery Agent assignment statuses
    DELIVERY_OFFER_RECEIVED,
    AGENT_ASSIGNED,
    NO_AGENT_FOUND,
}
