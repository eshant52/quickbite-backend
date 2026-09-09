package com.quickbite.quickbite.review.listener;

import com.quickbite.quickbite.common.event.review.ReviewChangedEvent;
import com.quickbite.quickbite.restaurant.exception.RestaurantNotFoundException;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.quickbite.review.repository.ReviewRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Domain event listener that handles {@link ReviewChangedEvent} to recalculate
 * and update restaurant rating aggregates (avgRating, totalRating) in a single place.
 */
@Component
public class RestaurantRatingEventListener {

    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;

    public RestaurantRatingEventListener(
            ReviewRepository reviewRepository,
            RestaurantRepository restaurantRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @EventListener
    public void onReviewChanged(ReviewChangedEvent event) {
        UUID restaurantId = event.restaurantId();

        // 1. Fetch the restaurant with a pessimistic lock to prevent concurrent updates
        Restaurant restaurant = restaurantRepository.findByIdForUpdate(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException("Restaurant not found with id: " + restaurantId));

        // 2. Calculate aggregate AFTER lock is acquired to ensure consistency
        Double avg = reviewRepository.getAverageRatingForRestaurant(restaurantId);
        long count = reviewRepository.countByRestaurantId(restaurantId);

        BigDecimal avgBigDecimal = avg != null
                ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        restaurant.setAvgRating(avgBigDecimal);
        restaurant.setTotalRating(count);
        restaurantRepository.save(restaurant);
    }
}
