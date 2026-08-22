package com.quickbite.quickbite.menu.repository;

import com.quickbite.quickbite.menu.model.MenuItem;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    @Query("""
    SELECT m FROM MenuItem m
    WHERE m.restaurant.id = :restaurantId
        AND (:cursor IS NULL OR m.id > :cursor)
        AND (:availableOnly = false OR m.isAvailable = true)
    ORDER BY m.id ASC
    """)
    List<MenuItem> findByRestaurantWithCursor(
            @Param("restaurantId") UUID restaurantId,
            @Param("availableOnly") boolean availableOnly,
            @Param("cursor") UUID cursor,
            Limit limit
    );

    Optional<MenuItem> findByIdAndRestaurantId(UUID id, UUID restaurantId);
}
