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
 * In-app notification for vehicle application lifecycle events.
 * <p>
 * Decoupled from the entity lifecycle by storing plain {@code applicationId} and snapshot {@code vehicleName}.
 */
@Getter
@Setter
@Entity
@Table(name = "vehicle_application_notifications")
public class VehicleApplicationNotification extends Notification {

    @Column(nullable = false)
    private UUID applicationId;

    @Column(length = 100)
    private String vehicleName;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "vehicle_application_notification_type", nullable = false)
    private VehicleApplicationNotificationType type;
}
