package com.quickbite.quickbite.review.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.common.event.review.ReviewChangedEvent;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.review.dto.CreateReviewRequest;
import com.quickbite.quickbite.review.dto.ReviewResponse;
import com.quickbite.quickbite.review.dto.UpdateReviewRequest;
import com.quickbite.quickbite.review.exception.DuplicateReviewException;
import com.quickbite.quickbite.review.exception.InvalidReviewException;
import com.quickbite.quickbite.review.exception.ReviewNotFoundException;
import com.quickbite.quickbite.review.model.Review;
import com.quickbite.quickbite.review.repository.ReviewRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CustomerReviewServiceImpl implements CustomerReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CustomerReviewServiceImpl(
            ReviewRepository reviewRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
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

        // Publish domain event to recalculate restaurant ratings asynchronously/via listener
        eventPublisher.publishEvent(new ReviewChangedEvent(restaurant.getId()));

        return ReviewResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReview(UUID reviewId, UUID customerId) {
        Review review = reviewRepository.findByIdAndCustomerId(reviewId, customerId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        return ReviewResponse.from(review);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ReviewResponse> getMyReviews(UUID customerId, UUID cursor, int size) {
        int fetchSize = Math.clamp(size, 1, 50);
        List<Review> fetched = reviewRepository.findByCustomerWithCursor(
                customerId,
                cursor,
                Limit.of(fetchSize + 1)
        );
        return CursorPage.of(fetched, fetchSize, Review::getId)
                .map(ReviewResponse::from);
    }

    @Override
    public ReviewResponse updateReview(UUID reviewId, UUID customerId, UpdateReviewRequest request) {
        Review review = reviewRepository.findByIdAndCustomerId(reviewId, customerId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId + " for this customer."));

        review.setRating(request.rating());
        review.setComment(request.comment());

        Review updated = reviewRepository.save(review);

        // Publish domain event to recalculate restaurant ratings
        eventPublisher.publishEvent(new ReviewChangedEvent(review.getRestaurant().getId()));

        return ReviewResponse.from(updated);
    }

    @Override
    public void deleteReview(UUID reviewId, UUID customerId) {
        Review review = reviewRepository.findByIdAndCustomerId(reviewId, customerId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId + " for this customer."));

        UUID restaurantId = review.getRestaurant().getId();
        reviewRepository.delete(review);

        // Publish domain event to recalculate restaurant ratings
        eventPublisher.publishEvent(new ReviewChangedEvent(restaurantId));
    }
}
