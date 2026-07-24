package com.quickbite.quickbite.payment.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.payment.model.PaymentStatusHistory;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Audited
@Entity
@Table(name = "payments")
public class Payment extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private Order order;

    @NotBlank(message = "Transaction ID is required")
    @Column(length = 255, unique = true, nullable = false)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(precision = 10, scale = 2, nullable = false)
    @Digits(integer = 8, fraction = 2, message = "Amount must have up to 8 digits and 2 decimal places")
    @DecimalMin(value = "0.01", message = "Amount must be greater than or equal to 0.01")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "payment_status", nullable = false)
    private PaymentStatus currentStatus = PaymentStatus.PENDING;

    @OneToMany(mappedBy = "payment")
    @NotAudited
    private List<PaymentStatusHistory> statusHistory;
}
