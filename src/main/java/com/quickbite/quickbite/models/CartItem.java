package com.quickbite.quickbite.models;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
public class CartItem extends Base {
    @ManyToOne
    private Cart cart;

    @ManyToOne
    private MenuItem menuItem;

    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
}
