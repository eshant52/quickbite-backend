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
 * In-app notification for delivery agent onboarding lifecycle events.
 * <p>
 * Decoupled from the entity lifecycle by storing plain {@code applicationId} and snapshot {@code agentName}.
 */
@Getter
@Setter
@Entity
@Table(name = "delivery_agent_application_notifications")
public class DeliveryAgentApplicationNotification extends Notification {

    @Column(nullable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "delivery_agent_application_notification_type", nullable = false)
    private DeliveryAgentApplicationNotificationType type;

    @Column(length = 150)
    private String agentName;
}
