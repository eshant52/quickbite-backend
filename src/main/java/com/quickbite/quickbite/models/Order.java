package com.quickbite.quickbite.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
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

    @OneToMany(mappedBy = "order")
    private List<OrderItem> items;

    @OneToMany(mappedBy = "order")
    private List<OrderStatusHistory> statusHistory;
}
