package com.quickbite.quickbite.review.repository;

import com.quickbite.quickbite.review.model.Review;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByOrderId(UUID orderId);

    Optional<Review> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<Review> findByOrderId(UUID orderId);

    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.customer
            JOIN FETCH r.restaurant
            JOIN FETCH r.order
            WHERE r.restaurant.id = :restaurantId
            ORDER BY r.id DESC
            """)
    List<Review> findByRestaurantIdOrderByIdDesc(@Param("restaurantId") UUID restaurantId, Limit limit);

    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.customer
            JOIN FETCH r.restaurant
            JOIN FETCH r.order
            WHERE r.restaurant.id = :restaurantId
              AND r.id < :cursor
            ORDER BY r.id DESC
            """)
    List<Review> findByRestaurantIdAndIdLessThanOrderByIdDesc(
            @Param("restaurantId") UUID restaurantId,
            @Param("cursor") UUID cursor,
            Limit limit
    );

    default List<Review> findByRestaurantWithCursor(
            UUID restaurantId,
            UUID cursor,
            Limit limit
    ) {
        return cursor == null
                ? findByRestaurantIdOrderByIdDesc(restaurantId, limit)
                : findByRestaurantIdAndIdLessThanOrderByIdDesc(restaurantId, cursor, limit);
    }

    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.customer
            JOIN FETCH r.restaurant
            JOIN FETCH r.order
            WHERE r.customer.id = :customerId
            ORDER BY r.id DESC
            """)
    List<Review> findByCustomerIdOrderByIdDesc(@Param("customerId") UUID customerId, Limit limit);

    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.customer
            JOIN FETCH r.restaurant
            JOIN FETCH r.order
            WHERE r.customer.id = :customerId
              AND r.id < :cursor
            ORDER BY r.id DESC
            """)
    List<Review> findByCustomerIdAndIdLessThanOrderByIdDesc(
            @Param("customerId") UUID customerId,
            @Param("cursor") UUID cursor,
            Limit limit
    );

    default List<Review> findByCustomerWithCursor(
            UUID customerId,
            UUID cursor,
            Limit limit
    ) {
        return cursor == null
                ? findByCustomerIdOrderByIdDesc(customerId, limit)
                : findByCustomerIdAndIdLessThanOrderByIdDesc(customerId, cursor, limit);
    }

    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.customer
            JOIN FETCH r.restaurant
            JOIN FETCH r.order
            ORDER BY r.id DESC
            """)
    List<Review> findAllFetchOrderByIdDesc(Limit limit);

    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.customer
            JOIN FETCH r.restaurant
            JOIN FETCH r.order
            WHERE r.id < :cursor
            ORDER BY r.id DESC
            """)
    List<Review> findAllFetchAndIdLessThanOrderByIdDesc(@Param("cursor") UUID cursor, Limit limit);

    default List<Review> findAllWithCursor(
            UUID restaurantId,
            UUID cursor,
            Limit limit
    ) {
        if (restaurantId != null) {
            return cursor == null
                    ? findByRestaurantIdOrderByIdDesc(restaurantId, limit)
                    : findByRestaurantIdAndIdLessThanOrderByIdDesc(restaurantId, cursor, limit);
        } else {
            return cursor == null
                    ? findAllFetchOrderByIdDesc(limit)
                    : findAllFetchAndIdLessThanOrderByIdDesc(cursor, limit);
        }
    }

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.restaurant.id = :restaurantId")
    Double getAverageRatingForRestaurant(@Param("restaurantId") UUID restaurantId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.restaurant.id = :restaurantId")
    long countByRestaurantId(@Param("restaurantId") UUID restaurantId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.restaurant.id = :restaurantId GROUP BY r.rating")
    List<Object[]> getRatingDistributionForRestaurant(@Param("restaurantId") UUID restaurantId);
}
