package com.quickbite.quickbite.cart.repository;

import com.quickbite.quickbite.cart.model.Cart;
import com.quickbite.quickbite.cart.model.CartItem;
import com.quickbite.quickbite.menu.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    Optional<CartItem> findByIdAndCart(UUID id, Cart cart);
    Optional<CartItem> findByCartAndMenuItem(Cart cart, MenuItem menuItem);
}
