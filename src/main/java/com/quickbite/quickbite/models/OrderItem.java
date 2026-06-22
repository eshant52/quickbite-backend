package com.quickbite.quickbite.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
public class OrderItem extends Base {
    @ManyToOne
    private MenuItem menuItem;

    @ManyToOne
    private Order order;
    
    private int  quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
}
