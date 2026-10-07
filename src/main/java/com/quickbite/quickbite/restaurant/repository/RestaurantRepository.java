package com.quickbite.quickbite.restaurant.repository;

import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.model.RestaurantVerificationStatus;
import com.quickbite.quickbite.user.model.User;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
    Optional<Restaurant> findByIdAndOwner(UUID id, User owner);

    List<Restaurant> findByOwnerAndCurrentStatusOrderByIdDesc(
            User owner,
            RestaurantVerificationStatus status,
            Limit limit
    );

    List<Restaurant> findByOwnerAndCurrentStatusAndIdLessThanOrderByIdDesc(
            User owner,
            RestaurantVerificationStatus status,
            UUID cursor,
            Limit limit
    );

    List<Restaurant> findByOwnerOrderByIdDesc(
            User owner,
            Limit limit
    );

    List<Restaurant> findByOwnerAndIdLessThanOrderByIdDesc(
            User owner,
            UUID cursor,
            Limit limit
    );

    default List<Restaurant> findByOwnerWithCursor(
            User owner,
            RestaurantVerificationStatus status,
            UUID cursor,
            Limit limit
    ) {
        if (status != null) {
            return cursor == null
                    ? findByOwnerAndCurrentStatusOrderByIdDesc(owner, status, limit)
                    : findByOwnerAndCurrentStatusAndIdLessThanOrderByIdDesc(owner, status, cursor, limit);
        } else {
            return cursor == null
                    ? findByOwnerOrderByIdDesc(owner, limit)
                    : findByOwnerAndIdLessThanOrderByIdDesc(owner, cursor, limit);
        }
    }

    List<Restaurant> findByCurrentStatusOrderByIdDesc(
            RestaurantVerificationStatus status,
            Limit limit
    );

    List<Restaurant> findByCurrentStatusAndIdLessThanOrderByIdDesc(
            RestaurantVerificationStatus status,
            UUID cursor,
            Limit limit
    );

    List<Restaurant> findAllByOrderByIdDesc(Limit limit);

    List<Restaurant> findByIdLessThanOrderByIdDesc(UUID cursor, Limit limit);

    default List<Restaurant> findAllWithCursor(
            RestaurantVerificationStatus status,
            UUID cursor,
            Limit limit
    ) {
        if (status != null) {
            return cursor == null
                    ? findByCurrentStatusOrderByIdDesc(status, limit)
                    : findByCurrentStatusAndIdLessThanOrderByIdDesc(status, cursor, limit);
        } else {
            return cursor == null
                    ? findAllByOrderByIdDesc(limit)
                    : findByIdLessThanOrderByIdDesc(cursor, limit);
        }
    }

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


    /**
     * Finds a restaurant by ID with a pessimistic write lock.
     * A scenario where no other transaction can read and write, preventing race conditions during updates.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000") // 3 seconds
    })
    @Query("SELECT r FROM Restaurant r WHERE r.id = :id")
    Optional<Restaurant> findByIdForUpdate(@Param("id") UUID id);
}
