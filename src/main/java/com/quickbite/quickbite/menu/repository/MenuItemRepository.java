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
    JOIN FETCH m.cuisine
    WHERE m.restaurant.id = :restaurantId
        AND m.isAvailable = true
    ORDER BY m.id ASC
    """)
    List<MenuItem> findAvailableByRestaurantOrderByIdAsc(@Param("restaurantId") UUID restaurantId, Limit limit);

    @Query("""
    SELECT m FROM MenuItem m
    JOIN FETCH m.cuisine
    WHERE m.restaurant.id = :restaurantId
        AND m.isAvailable = true
        AND m.id > :cursor
    ORDER BY m.id ASC
    """)
    List<MenuItem> findAvailableByRestaurantAndIdGreaterThanOrderByIdAsc(
            @Param("restaurantId") UUID restaurantId,
            @Param("cursor") UUID cursor,
            Limit limit
    );

    @Query("""
    SELECT m FROM MenuItem m
    JOIN FETCH m.cuisine
    WHERE m.restaurant.id = :restaurantId
    ORDER BY m.id ASC
    """)
    List<MenuItem> findAllByRestaurantOrderByIdAsc(@Param("restaurantId") UUID restaurantId, Limit limit);

    @Query("""
    SELECT m FROM MenuItem m
    JOIN FETCH m.cuisine
    WHERE m.restaurant.id = :restaurantId
        AND m.id > :cursor
    ORDER BY m.id ASC
    """)
    List<MenuItem> findAllByRestaurantAndIdGreaterThanOrderByIdAsc(
            @Param("restaurantId") UUID restaurantId,
            @Param("cursor") UUID cursor,
            Limit limit
    );

    default List<MenuItem> findByRestaurantWithCursor(
            UUID restaurantId,
            boolean availableOnly,
            UUID cursor,
            Limit limit
    ) {
        if (availableOnly) {
            return cursor == null
                    ? findAvailableByRestaurantOrderByIdAsc(restaurantId, limit)
                    : findAvailableByRestaurantAndIdGreaterThanOrderByIdAsc(restaurantId, cursor, limit);
        } else {
            return cursor == null
                    ? findAllByRestaurantOrderByIdAsc(restaurantId, limit)
                    : findAllByRestaurantAndIdGreaterThanOrderByIdAsc(restaurantId, cursor, limit);
        }
    }

    Optional<MenuItem> findByIdAndRestaurantId(UUID id, UUID restaurantId);
}
