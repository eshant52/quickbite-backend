package com.quickbite.quickbite.notification.model;

import com.quickbite.quickbite.notification.model.Notification;
import com.quickbite.quickbite.order.model.Order;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "order_notifications")
public class OrderNotification extends Notification {
    @ManyToOne
    @JoinColumn(nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "order_notification_type", nullable = false)
    private OrderNotificationType type;
}
