package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.Notification;
import com.quickbite.quickbite.notification.model.OrderNotification;
import com.quickbite.quickbite.notification.model.PaymentNotification;
import com.quickbite.quickbite.notification.model.RestaurantApplicationNotification;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * API response for a single notification.
 *
 * @param id           Notification ID — used by the client to call PUT /notifications/{id}/read.
 * @param title        Short subject line.
 * @param message      Full body text.
 * @param read         Whether the recipient has seen this notification.
 * @param category     High-level discriminator for client-side routing:
 *                     RESTAURANT_APPLICATION | ORDER | PAYMENT | UNKNOWN.
 * @param referenceId  Primary related entity ID (applicationId, orderId, paymentId).
 *                     Clients use this to deep-link to the relevant screen.
 * @param meta         Extra key-value data specific to the category
 *                     (e.g. {@code restaurantName} for RESTAURANT_APPLICATION).
 * @param createdAt    When the notification was created — use for display ordering.
 */
public record NotificationResponse(
        UUID id,
        String title,
        String message,
        boolean read,
        String category,
        UUID referenceId,
        Map<String, String> meta,
        Instant createdAt
) {
    /**
     * Converts any {@link Notification} subtype into this generic response.
     * Uses Java pattern matching to branch on the concrete subtype and extract
     * domain-specific fields (referenceId, meta) without exposing JPA entities.
     */
    public static NotificationResponse from(Notification n) {
        return switch (n) {
            case RestaurantApplicationNotification ran -> new NotificationResponse(
                    ran.getId(),
                    ran.getTitle(),
                    ran.getMessage(),
                    ran.isRead(),
                    "RESTAURANT_APPLICATION",
                    ran.getApplicationId(),
                    Map.of("restaurantName", Objects.requireNonNullElse(ran.getRestaurantName(), ""),
                            "type", ran.getType().name()),
                    ran.getCreatedAt()
            );
            case OrderNotification on -> new NotificationResponse(
                    on.getId(),
                    on.getTitle(),
                    on.getMessage(),
                    on.isRead(),
                    "ORDER",
                    on.getOrder() != null ? on.getOrder().getId() : null,
                    Map.of("type", on.getType() != null ? on.getType().name() : ""),
                    on.getCreatedAt()
            );
            case PaymentNotification pn -> new NotificationResponse(
                    pn.getId(),
                    pn.getTitle(),
                    pn.getMessage(),
                    pn.isRead(),
                    "PAYMENT",
                    pn.getPayment() != null ? pn.getPayment().getId() : null,
                    Map.of("type", pn.getType() != null ? pn.getType().name() : ""),
                    pn.getCreatedAt()
            );
            default -> new NotificationResponse(
                    n.getId(),
                    n.getTitle(),
                    n.getMessage(),
                    n.isRead(),
                    "UNKNOWN",
                    null,
                    Map.of(),
                    n.getCreatedAt()
            );
        };
    }
}
