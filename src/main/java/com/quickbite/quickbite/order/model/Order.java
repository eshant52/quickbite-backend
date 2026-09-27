package com.quickbite.quickbite.order.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Audited
@Entity
@Table(
        name = "orders",
        indexes = {
                // Partial index in PostgreSQL: ON orders (created_at) WHERE current_status = 'AWAITING_PAYMENT'
                @Index(name = "idx_orders_awaiting_payment_created", columnList = "created_at"),
                @Index(name = "idx_orders_restaurant_acceptance_deadline", columnList = "restaurant_acceptance_deadline")
        }
)
public class Order extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private Restaurant restaurant;

    @ManyToOne
    @JoinColumn(nullable = false)
    private User customer;

    @ManyToOne
    @JoinColumn
    private DeliveryAgent deliveryAgent;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String deliveryAddress;

    @Column(columnDefinition = "GEOMETRY(POINT, 4326)")
    @JdbcTypeCode(SqlTypes.GEOMETRY)
    private Point deliveryLocation;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal discountAmount;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal deliveryFee;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal platformFee;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal taxAmount;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal tipAmount;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "order_status", nullable = false)
    private OrderStatus currentStatus = OrderStatus.PLACED;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    private List<OrderItem> items;

    @OneToMany(mappedBy = "order")
    @OrderBy("createdAt ASC")
    @NotAudited
    private List<OrderStatusHistory> statusHistory = new java.util.ArrayList<>();

    /** Road-network delivery distance in metres, resolved at order creation. */
    @Column
    private Double deliveryDistanceMeters;

    /** Estimated driving duration from restaurant to customer in seconds. */
    @Column
    private Long estimatedDeliverySeconds;

    @Column
    private Instant restaurantAcceptanceDeadline;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "order_cancellation_reason")
    private OrderCancellationReason cancellationReason;

    @Version
    @Column(nullable = false)
    private Long version;
}
