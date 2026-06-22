package com.quickbite.quickbite.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Entity
public class Cart extends Base {
    @OneToOne
    private User customer;

    @ManyToOne
    private Restaurant restaurant;

    @OneToMany(mappedBy = "cart")
    private List<CartItem> items;

    private BigDecimal totalPrice;
    private Instant expiresAt;
}
