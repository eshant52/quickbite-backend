package com.quickbite.quickbite.common.event;

public final class QuickBiteTopics {

    public static final String ORDER_EVENTS     = "quickbite.order.events";
    public static final String DELIVERY_EVENTS  = "quickbite.delivery.events";
    public static final String ORDER_EVENTS_DLQ = "quickbite.order.events.DLQ";

    // Notification pipeline
    public static final String NOTIFICATION_EVENTS = "quickbite.notification.events";
    public static final String NOTIFICATION_DLT    = "quickbite.notification.DLT";

    // Restaurant onboarding application events
    public static final String RESTAURANT_APPLICATION_SUBMITTED = "quickbite.restaurant.application.submitted";
    public static final String RESTAURANT_APPLICATION_APPROVED  = "quickbite.restaurant.application.approved";
    public static final String RESTAURANT_APPLICATION_REJECTED  = "quickbite.restaurant.application.rejected";

    // Cuisine request events (owner requests → admin approves/rejects)
    public static final String CUISINE_REQUESTED = "quickbite.cuisine.requested";
    public static final String CUISINE_APPROVED  = "quickbite.cuisine.approved";
    public static final String CUISINE_REJECTED  = "quickbite.cuisine.rejected";

    private QuickBiteTopics() {}
}