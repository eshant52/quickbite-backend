package com.quickbite.quickbite.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "cart_items")
public class CartItem extends Base {
    @ManyToOne
    @JoinColumn(nullable = false)
    private Cart cart;

    @ManyToOne
    @JoinColumn(nullable = false)
    private MenuItem menuItem;

    @Column(nullable = false)
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    @Column(precision = 10, scale = 2, nullable = false)
    @Digits(integer = 8, fraction = 2, message = "Unit price must have up to 8 digits and 2 decimal places")
    @DecimalMin(value = "0.01", message = "Unit price must be greater than or equal to 0.01")
    private BigDecimal unitPrice;

    @Column(precision = 10, scale = 2, nullable = false)
    @Digits(integer = 8, fraction = 2, message = "Subtotal must have up to 8 digits and 2 decimal places")
    @DecimalMin(value = "0.01", message = "Subtotal must be greater than or equal to 0.01")
    private BigDecimal subTotal;
}
