package com.quickbite.quickbite.notification.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.quickbite.quickbite.notification.model.CuisineNotification;
import com.quickbite.quickbite.notification.model.Notification;
import com.quickbite.quickbite.notification.model.OrderNotification;
import com.quickbite.quickbite.notification.model.PaymentNotification;
import com.quickbite.quickbite.notification.model.RestaurantApplicationNotification;

import java.util.UUID;

/**
 * API response for a single notification. This is a sealed interface with concrete implementations:
 * {@link OrderNotificationResponse}, {@link PaymentNotificationResponse},
 * {@link RestaurantApplicationNotificationResponse}, and {@link CuisineNotificationResponse}.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "category"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OrderNotificationResponse.class, name = "ORDER"),
        @JsonSubTypes.Type(value = PaymentNotificationResponse.class, name = "PAYMENT"),
        @JsonSubTypes.Type(value = RestaurantApplicationNotificationResponse.class, name = "RESTAURANT_APPLICATION"),
        @JsonSubTypes.Type(value = CuisineNotificationResponse.class, name = "CUISINE")
})
public sealed interface NotificationResponse permits OrderNotificationResponse,
        PaymentNotificationResponse, RestaurantApplicationNotificationResponse, CuisineNotificationResponse {
    UUID id();
    String title();
    String message();
    boolean isRead();

    /**
     * Factory method to create a NotificationResponse from a Notification entity.
     */
    static NotificationResponse from(Notification n) {
        return switch (n) {
            case RestaurantApplicationNotification ran -> RestaurantApplicationNotificationResponse.from(ran);
            case OrderNotification on -> OrderNotificationResponse.from(on);
            case PaymentNotification pn -> PaymentNotificationResponse.from(pn);
            case CuisineNotification cn -> CuisineNotificationResponse.from(cn);
            default -> throw new IllegalArgumentException("Unknown notification entity: " + n.getClass().getName());
        };
    }
}
