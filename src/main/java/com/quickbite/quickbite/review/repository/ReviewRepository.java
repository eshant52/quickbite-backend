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
            WHERE r.restaurant.id = :restaurantId
              AND (:cursor IS NULL OR r.id < :cursor)
            ORDER BY r.id DESC
            """)
    List<Review> findByRestaurantWithCursor(
            @Param("restaurantId") UUID restaurantId,
            @Param("cursor") UUID cursor,
            Limit limit
    );

    @Query("""
            SELECT r FROM Review r
            WHERE r.customer.id = :customerId
              AND (:cursor IS NULL OR r.id < :cursor)
            ORDER BY r.id DESC
            """)
    List<Review> findByCustomerWithCursor(
            @Param("customerId") UUID customerId,
            @Param("cursor") UUID cursor,
            Limit limit
    );

    @Query("""
            SELECT r FROM Review r
            WHERE (:restaurantId IS NULL OR r.restaurant.id = :restaurantId)
              AND (:cursor IS NULL OR r.id < :cursor)
            ORDER BY r.id DESC
            """)
    List<Review> findAllWithCursor(
            @Param("restaurantId") UUID restaurantId,
            @Param("cursor") UUID cursor,
            Limit limit
    );

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.restaurant.id = :restaurantId")
    Double getAverageRatingForRestaurant(@Param("restaurantId") UUID restaurantId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.restaurant.id = :restaurantId")
    long countByRestaurantId(@Param("restaurantId") UUID restaurantId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.restaurant.id = :restaurantId GROUP BY r.rating")
    List<Object[]> getRatingDistributionForRestaurant(@Param("restaurantId") UUID restaurantId);
}
