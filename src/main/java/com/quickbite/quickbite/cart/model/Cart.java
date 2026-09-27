package com.quickbite.quickbite.cart.model;

import com.quickbite.quickbite.common.model.Base;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
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

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private Instant expiresAt;

    public void addItem(CartItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
        item.setCart(this);
    }

    public void removeItem(CartItem item) {
        if (this.items == null) return;
        this.items.remove(item);
        item.setCart(null);
    }
}
