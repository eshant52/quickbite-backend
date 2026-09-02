package com.quickbite.quickbite.review.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.restaurant.exception.RestaurantNotFoundException;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.quickbite.review.dto.ReviewResponse;
import com.quickbite.quickbite.review.exception.ReviewNotFoundException;
import com.quickbite.quickbite.review.model.Review;
import com.quickbite.quickbite.review.repository.ReviewRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminReviewServiceImpl implements AdminReviewService {

    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;

    public AdminReviewServiceImpl(
            ReviewRepository reviewRepository,
            RestaurantRepository restaurantRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ReviewResponse> listAllReviews(UUID restaurantId, UUID cursor, int size) {
        int fetchSize = Math.max(1, Math.min(size, 50));
        List<Review> fetched = reviewRepository.findAllWithCursor(
                restaurantId,
                cursor,
                Limit.of(fetchSize + 1)
        );
        return CursorPage.of(fetched, fetchSize, Review::getId)
                .map(ReviewResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewAsAdmin(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        return ReviewResponse.from(review);
    }

    @Override
    public void deleteReviewAsAdmin(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        UUID restaurantId = review.getRestaurant().getId();
        reviewRepository.delete(review);

        // Recalculate restaurant ratings
        updateRestaurantRatingAggregate(restaurantId);
    }

    private void updateRestaurantRatingAggregate(UUID restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException("Restaurant not found with id: " + restaurantId));

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
