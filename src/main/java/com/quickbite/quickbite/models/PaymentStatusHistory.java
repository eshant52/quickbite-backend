package com.quickbite.quickbite.models;

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
@Table(name = "payment_status_history")
public class PaymentStatusHistory extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "payment_status", nullable = false)
    private PaymentStatus status;
}
