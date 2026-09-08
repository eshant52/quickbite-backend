package com.quickbite.quickbite.review.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.restaurant.exception.RestaurantNotFoundException;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.quickbite.review.dto.RestaurantRatingSummaryResponse;
import com.quickbite.quickbite.review.dto.ReviewResponse;
import com.quickbite.quickbite.review.model.Review;
import com.quickbite.quickbite.review.repository.ReviewRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RestaurantReviewQueryServiceImpl implements RestaurantReviewQueryService {

    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;

    public RestaurantReviewQueryServiceImpl(
            ReviewRepository reviewRepository,
            RestaurantRepository restaurantRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Override
    public CursorPage<ReviewResponse> getRestaurantReviews(UUID restaurantId, UUID cursor, int size) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new RestaurantNotFoundException("Restaurant not found with id: " + restaurantId);
        }

        int fetchSize = Math.clamp(size, 1, 50);
        List<Review> fetched = reviewRepository.findByRestaurantWithCursor(
                restaurantId,
                cursor,
                Limit.of(fetchSize + 1)
        );
        return CursorPage.of(fetched, fetchSize, Review::getId)
                .map(ReviewResponse::from);
    }

    @Override
    public RestaurantRatingSummaryResponse getRestaurantRatingSummary(UUID restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException("Restaurant not found with id: " + restaurantId));

        List<Object[]> distributionList = reviewRepository.getRatingDistributionForRestaurant(restaurantId);
        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }

        for (Object[] row : distributionList) {
            Integer star = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            distribution.put(star, count);
        }

        return new RestaurantRatingSummaryResponse(
                restaurantId,
                restaurant.getAvgRating() != null ? restaurant.getAvgRating() : BigDecimal.ZERO,
                restaurant.getTotalRating() != null ? restaurant.getTotalRating() : 0L,
                distribution
        );
    }
}
