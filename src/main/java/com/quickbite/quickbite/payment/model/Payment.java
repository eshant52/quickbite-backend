package com.quickbite.quickbite.payment.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.order.model.Order;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uc_payments_transaction_id", columnNames = "transaction_id")
        },
        indexes = {
                @Index(name = "idx_payments_gateway_order_id", columnList = "gateway_order_id")
        }
)
public class Payment extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private Order order;

    @Column(unique = true, nullable = false)
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

    /**
     * Name of the payment gateway used for this payment (e.g., "Razorpay", "Stripe").
     * Null for COD payments.
     */
    @Column(length = 50)
    private String gatewayName;

    /**
     * Gateway-specific metadata stored as JSONB.
     * This may include fields like JSON responses from the payment gateway, error codes,
     * or any other relevant information.
     * It is used for debugging and auditing purposes, and can be queried directly in PostgreSQL.
     */
    @Column(columnDefinition = "jsonb")
    private String gatewayMetaData;

    /**
     * Gateway-assigned order identifier (e.g. Razorpay "order_xxx").
     * Set during payment initiation by {@link com.quickbite.quickbite.payment.service.strategy.OnlinePaymentStrategy}.
     * Used to correlate webhook and client-side verification events back to this payment.
     * Null for COD payments.
     */
    private String gatewayOrderId;

    /**
     * Gateway-assigned payment identifier (e.g. Razorpay "pay_xxx").
     * Populated after successful client-side or webhook verification.
     * Null for COD payments and unconfirmed online payments.
     */
    private String gatewayPaymentId;

    @OneToMany(mappedBy = "payment")
    @NotAudited
    private List<PaymentStatusHistory> statusHistory;

    @Version
    @Column(nullable = false)
    private Long version;
}
