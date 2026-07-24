package com.quickbite.quickbite.notification.model;

import com.quickbite.quickbite.notification.model.Notification;
import com.quickbite.quickbite.payment.model.Payment;
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
@Table(name = "payment_notifications")
public class PaymentNotification extends Notification {
    @ManyToOne
    @JoinColumn(nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "payment_notification_type", nullable = false)
    private PaymentNotificationType type;
}
