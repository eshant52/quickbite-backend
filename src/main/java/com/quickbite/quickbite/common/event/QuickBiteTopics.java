package com.quickbite.quickbite.common.event;

public final class QuickBiteTopics {

    public static final String ORDER_EVENTS = "quickbite.order.events";
    public static final String NOTIFICATION_EVENTS = "quickbite.notification.events";
    public static final String DELIVERY_EVENTS = "quickbite.delivery.events";
    public static final String ORDER_EVENTS_DLQ = "quickbite.order.events.DLQ";

    private QuickBiteTopics() {
    }
}