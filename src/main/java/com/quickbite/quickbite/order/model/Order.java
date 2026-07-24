package com.quickbite.quickbite.order.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.order.model.OrderItem;
import com.quickbite.quickbite.order.model.OrderStatusHistory;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
import java.util.List;

@Getter
@Setter
@Audited
@Entity
@Table(name = "orders")
public class Order extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private Restaurant restaurant;

    @ManyToOne
    @JoinColumn(nullable = false)
    private User customer;

    @ManyToOne
    @JoinColumn(nullable = true)
    private DeliveryAgent deliveryAgent;

    @NotBlank(message = "Delivery address is required")
    @Size(max = 500, message = "Delivery address must be at most 500 characters")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String deliveryAddress;

    @Column(columnDefinition = "GEOMETRY(POINT, 4326)")
    @JdbcTypeCode(SqlTypes.GEOMETRY)
    private Point deliveryLocation;

    @Column(precision = 10, scale = 2, nullable = false)
    @Digits(integer = 8, fraction = 2, message = "Sub total must have up to 8 digits and 2 decimal places")
    @DecimalMin(value = "0.01", message = "Sub total must be greater than or equal to 0.01")
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2, nullable = false)
    @Digits(integer = 8, fraction = 2, message = "Discount amount must have up to 8 digits and 2 decimal places")
    @DecimalMin(value = "0.00", message = "Discount amount must be greater than or equal to 0.00")
    private BigDecimal discountAmount;

    @Column(precision = 10, scale = 2, nullable = false)
    @Digits(integer = 8, fraction = 2, message = "Delivery fee must have up to 8 digits and 2 decimal places")
    @DecimalMin(value = "0.00", message = "Delivery fee must be greater than or equal to 0.00")
    private BigDecimal deliveryFee;

    @Column(precision = 10, scale = 2, nullable = false)
    @Digits(integer = 8, fraction = 2, message = "Platform fee must have up to 8 digits and 2 decimal places")
    @DecimalMin(value = "0.00", message = "Platform fee must be greater than or equal to 0.00")
    private BigDecimal platformFee;

    @Column(precision = 10, scale = 2, nullable = false)
    @Digits(integer = 8, fraction = 2, message = "Tax amount must have up to 8 digits and 2 decimal places")
    @DecimalMin(value = "0.00", message = "Tax amount must be greater than or equal to 0.00")
    private BigDecimal taxAmount;

    @Column(precision = 10, scale = 2, nullable = false)
    @Digits(integer = 8, fraction = 2, message = "Tip amount must have up to 8 digits and 2 decimal places")
    @DecimalMin(value = "0.00", message = "Tip amount must be greater than or equal to 0.00")
    private BigDecimal tipAmount;

    @Column(precision = 10, scale = 2, nullable = false)
    @Digits(integer = 8, fraction = 2, message = "Total amount must have up to 8 digits and 2 decimal places")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than or equal to 0.01")
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(columnDefinition = "order_status", nullable = false)
    private OrderStatus currentStatus = OrderStatus.PLACED;

    @OneToMany(mappedBy = "order")
    @NotAudited
    private List<OrderItem> items;

    @OneToMany(mappedBy = "order")
    @NotAudited
    private List<OrderStatusHistory> statusHistory;
}
