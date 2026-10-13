package com.quickbite.quickbite.cart.repository;

import com.quickbite.quickbite.cart.model.Cart;
import com.quickbite.quickbite.user.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByCustomer(User customer);

    /**
     * Loads the cart together with everything checkout pricing needs (items, their menu items,
     * the restaurant and its address) in a single short query.
     *
     * <p>Callers can then work with the detached aggregate <em>outside</em> any transaction
     * (e.g. while making routing/Redis calls) without hitting lazy-loading errors or holding a
     * DB connection open.
     */
    @EntityGraph(attributePaths = {"items", "items.menuItem", "restaurant", "restaurant.address"})
    Optional<Cart> findDetailedByCustomer(User customer);
}
