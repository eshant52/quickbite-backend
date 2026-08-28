package com.quickbite.quickbite.restaurant.repository;

import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.model.RestaurantVerificationStatus;
import com.quickbite.quickbite.user.model.User;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
    Optional<Restaurant> findByIdAndOwner(UUID id, User owner);

    @Query("""
                SELECT r FROM Restaurant r
                WHERE r.owner = :owner
                  AND (:status IS NULL OR r.currentStatus = :status)
                  AND (:cursor IS NULL OR r.id < :cursor)
                ORDER BY r.id DESC
            """)
    List<Restaurant> findByOwnerWithCursor(
            @Param("owner") User owner,
            @Param("status") RestaurantVerificationStatus status,
            @Param("cursor") UUID cursor,
            Limit limit
    );

    @Query("""
                SELECT r FROM Restaurant r
                WHERE (:status IS NULL OR r.currentStatus = :status)
                  AND (:cursor IS NULL OR r.id < :cursor)
                ORDER BY r.id DESC
            """)
    List<Restaurant> findAllWithCursor(
            @Param("status") RestaurantVerificationStatus status,
            @Param("cursor") UUID cursor,
            Limit limit
    );
}
