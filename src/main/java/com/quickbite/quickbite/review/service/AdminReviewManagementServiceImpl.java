package com.quickbite.quickbite.review.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.common.event.review.ReviewChangedEvent;
import com.quickbite.quickbite.review.dto.ReviewResponse;
import com.quickbite.quickbite.review.exception.ReviewNotFoundException;
import com.quickbite.quickbite.review.model.Review;
import com.quickbite.quickbite.review.repository.ReviewRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminReviewManagementServiceImpl implements AdminReviewManagementService {

    private final ReviewRepository reviewRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AdminReviewManagementServiceImpl(
            ReviewRepository reviewRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.reviewRepository = reviewRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ReviewResponse> listReviews(UUID restaurantId, UUID cursor, int size) {
        int fetchSize = Math.clamp(size, 1, 50);
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
    public ReviewResponse getReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        return ReviewResponse.from(review);
    }

    @Override
    public void deleteReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        UUID restaurantId = review.getRestaurant().getId();
        reviewRepository.delete(review);

        // Publish domain event to recalculate restaurant ratings
        eventPublisher.publishEvent(new ReviewChangedEvent(restaurantId));
    }
}

