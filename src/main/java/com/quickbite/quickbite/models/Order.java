package com.quickbite.quickbite.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Entity
public class Order extends Base {
    @ManyToOne
    private Restaurant restaurant;

    @ManyToOne
    private User customer;

    @ManyToOne
    private DeliveryAgent deliveryAgent;

    private String deliveryAddress;
    private Point deliveryLocation;

    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;
    private BigDecimal platformFee;
    private BigDecimal taxAmount;
    private BigDecimal tipAmount;
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order")
    private List<OrderItem> items;

    @OneToMany(mappedBy = "order")
    private List<OrderStatusHistory> statusHistory;
}
