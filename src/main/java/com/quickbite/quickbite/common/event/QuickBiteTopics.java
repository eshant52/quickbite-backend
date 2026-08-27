package com.quickbite.quickbite.common.event;

public final class QuickBiteTopics {

    // Domain aggregate event streams
    public static final String ORDER_EVENTS                  = "quickbite.order.events";
    public static final String RESTAURANT_APPLICATION_EVENTS = "quickbite.restaurant.application.events";
    public static final String CUISINE_EVENTS                = "quickbite.cuisine.events";
    public static final String PAYMENT_EVENTS                = "quickbite.payment.events";

    // Dead letter topic suffix convention: <topic>.DLT (managed by DeadLetterPublishingRecoverer)
    public static final String DLT_SUFFIX = ".DLT";

    private QuickBiteTopics() {}
}