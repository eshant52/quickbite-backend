package com.quickbite.quickbite.cart.repository;

import com.quickbite.quickbite.cart.model.Cart;
import com.quickbite.quickbite.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByCustomer(User customer);
}
