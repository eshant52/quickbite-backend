package com.quickbite.quickbite.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "carts")
public class Cart extends Base {
    @OneToOne
    @JoinColumn(nullable = false, unique = true)
    private User customer;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Restaurant restaurant;

    @OneToMany(mappedBy = "cart")
    private List<CartItem> items;

    @Column(precision = 10, scale = 2, nullable = false)
    @Digits(integer = 8, fraction = 2, message = "Total price must have up to 8 digits and 2 decimal places")
    @DecimalMin(value = "0.01", message = "Total price must be greater than or equal to 0.01")
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private Instant expiresAt;
}
