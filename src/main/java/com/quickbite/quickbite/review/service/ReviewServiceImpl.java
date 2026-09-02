package com.quickbite.quickbite.review.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.restaurant.exception.RestaurantNotFoundException;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.quickbite.review.dto.CreateReviewRequest;
import com.quickbite.quickbite.review.dto.RestaurantRatingSummaryResponse;
import com.quickbite.quickbite.review.dto.ReviewResponse;
import com.quickbite.quickbite.review.dto.UpdateReviewRequest;
import com.quickbite.quickbite.review.exception.DuplicateReviewException;
import com.quickbite.quickbite.review.exception.InvalidReviewException;
import com.quickbite.quickbite.review.exception.ReviewNotFoundException;
import com.quickbite.quickbite.review.model.Review;
import com.quickbite.quickbite.review.repository.ReviewRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    public ReviewServiceImpl(
            ReviewRepository reviewRepository,
            OrderRepository orderRepository,
            RestaurantRepository restaurantRepository,
            UserRepository userRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ReviewResponse submitReview(UUID customerId, CreateReviewRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new InvalidReviewException("Order not found with id: " + request.orderId()));

        if (!order.getCustomer().getId().equals(customerId)) {
            throw new InvalidReviewException("You can only review orders placed by your own account.");
        }

        if (order.getCurrentStatus() != OrderStatus.DELIVERED) {
            throw new InvalidReviewException("Reviews can only be submitted for completed (DELIVERED) orders. Current order status: " + order.getCurrentStatus());
        }

        if (reviewRepository.existsByOrderId(order.getId())) {
            throw new DuplicateReviewException(order.getId());
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new InvalidReviewException("Customer not found with id: " + customerId));

        Restaurant restaurant = order.getRestaurant();

        Review review = new Review();
        review.setRestaurant(restaurant);
        review.setCustomer(customer);
        review.setOrder(order);
        review.setRating(request.rating());
        review.setComment(request.comment());

        Review saved = reviewRepository.save(review);

        // Recalculate and update restaurant ratings
        updateRestaurantRatingAggregate(restaurant.getId());

        return ReviewResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        return ReviewResponse.from(review);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ReviewResponse> getMyReviews(UUID customerId, UUID cursor, int size) {
        int fetchSize = Math.max(1, Math.min(size, 50));
        List<Review> fetched = reviewRepository.findByCustomerWithCursor(
                customerId,
                cursor,
                Limit.of(fetchSize + 1)
        );
        return CursorPage.of(fetched, fetchSize, Review::getId)
                .map(ReviewResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ReviewResponse> getRestaurantReviews(UUID restaurantId, UUID cursor, int size) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new RestaurantNotFoundException("Restaurant not found with id: " + restaurantId);
        }

        int fetchSize = Math.max(1, Math.min(size, 50));
        List<Review> fetched = reviewRepository.findByRestaurantWithCursor(
                restaurantId,
                cursor,
                Limit.of(fetchSize + 1)
        );
        return CursorPage.of(fetched, fetchSize, Review::getId)
                .map(ReviewResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
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

    @Override
    public ReviewResponse updateReview(UUID reviewId, UUID customerId, UpdateReviewRequest request) {
        Review review = reviewRepository.findByIdAndCustomerId(reviewId, customerId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId + " for this customer."));

        review.setRating(request.rating());
        review.setComment(request.comment());

        Review updated = reviewRepository.save(review);

        // Recalculate restaurant ratings
        updateRestaurantRatingAggregate(review.getRestaurant().getId());

        return ReviewResponse.from(updated);
    }

    @Override
    public void deleteReview(UUID reviewId, UUID customerId) {
        Review review = reviewRepository.findByIdAndCustomerId(reviewId, customerId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId + " for this customer."));

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
