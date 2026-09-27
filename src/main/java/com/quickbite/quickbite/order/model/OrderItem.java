package com.quickbite.quickbite.order.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.menu.model.MenuItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "order_items")
public class OrderItem extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private MenuItem menuItem;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Order order;

    @Column(nullable = false)
    private int quantity;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal subTotal;
}
