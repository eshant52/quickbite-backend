package com.quickbite.quickbite.notification.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * In-app notification for restaurant onboarding lifecycle events.
 * <p>
 * Follows the same JOINED inheritance pattern as {@link OrderNotification}
 * and {@link PaymentNotification}.
 * <p>
 * Named specifically for restaurant applications so it is unambiguous when
 * vehicle registration, delivery-agent onboarding, or other application types
 * introduce their own notification subtypes.
 * <p>
 * {@code applicationId} is a plain UUID (no FK) because notifications are
 * historical records — they must survive even if the source application row
 * is deleted or archived.
 */
@Getter
@Setter
@Entity
@Table(name = "restaurant_application_notifications")
public class RestaurantApplicationNotification extends Notification {

    /** Reference to the RestaurantApplication — stored without a FK by design (see Javadoc). */
    @Column(nullable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "restaurant_application_notification_type", nullable = false)
    private RestaurantApplicationNotificationType type;

    /** Snapshot of the restaurant name at event time — stored denormalized for read convenience. */
    @Column(length = 200)
    private String restaurantName;
}
