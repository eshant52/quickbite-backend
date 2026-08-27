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
 * In-app notification for cuisine request lifecycle events.
 * <p>
 * Decoupled from the entity lifecycle by storing plain {@code cuisineRequestId} and snapshot {@code cuisineName}.
 */
@Getter
@Setter
@Entity
@Table(name = "cuisine_notifications")
public class CuisineNotification extends Notification {

    @Column(nullable = false)
    private UUID cuisineRequestId;

    @Column(length = 100, nullable = false)
    private String cuisineName;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "cuisine_notification_type", nullable = false)
    private CuisineNotificationType type;
}
