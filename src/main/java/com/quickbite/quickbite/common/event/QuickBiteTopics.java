package com.quickbite.quickbite.common.event;

public final class QuickBiteTopics {

    public static final String ORDER_EVENTS = "quickbite.order.events";
    public static final String NOTIFICATION_EVENTS = "quickbite.notification.events";
    public static final String DELIVERY_EVENTS = "quickbite.delivery.events";
    public static final String ORDER_EVENTS_DLQ = "quickbite.order.events.DLQ";

    // Restaurant onboarding events
    public static final String RESTAURANT_APPLICATION_SUBMITTED = "quickbite.restaurant.application.submitted";
    public static final String RESTAURANT_APPROVED = "quickbite.restaurant.approved";
    public static final String RESTAURANT_REJECTED = "quickbite.restaurant.rejected";

    // Dead-letter topics (DLT) — messages land here after all retries are exhausted
    public static final String NOTIFICATION_DLT = "quickbite.notification.DLT";

    private QuickBiteTopics() {
    }
}