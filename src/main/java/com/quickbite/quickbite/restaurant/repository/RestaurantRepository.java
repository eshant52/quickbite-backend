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

    /**
     * Finds approved, open restaurants within a given radius of a customer location.
     * Uses PostGIS {@code ST_DWithin} for radius filtering (uses spatial index)
     * and {@code ST_Distance} for distance-based ordering.
     *
     * <p>{@code ST_DWithin} on geography uses metres as the unit.
     */
    @Query(value = """
            SELECT r.* FROM restaurants r
            JOIN addresses a ON a.id = r.address_id
            WHERE r.current_status = 'APPROVED'
              AND r.is_closed = false
              AND ST_DWithin(
                    a.location::geography,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                    :radiusMeters
                  )
            ORDER BY ST_Distance(
                         a.location::geography,
                         ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
                     ) ASC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Restaurant> findNearbyRestaurants(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") int radiusMeters,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * Count of approved, open restaurants within radius — used for pagination metadata.
     */
    @Query(value = """
            SELECT COUNT(*) FROM restaurants r
            JOIN addresses a ON a.id = r.address_id
            WHERE r.current_status = 'APPROVED'
              AND r.is_closed = false
              AND ST_DWithin(
                    a.location::geography,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                    :radiusMeters
                  )
            """, nativeQuery = true)
    long countNearbyRestaurants(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") int radiusMeters
    );
}
