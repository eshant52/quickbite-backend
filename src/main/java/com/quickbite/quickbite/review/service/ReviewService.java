package com.quickbite.quickbite.review.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.review.dto.CreateReviewRequest;
import com.quickbite.quickbite.review.dto.RestaurantRatingSummaryResponse;
import com.quickbite.quickbite.review.dto.ReviewResponse;
import com.quickbite.quickbite.review.dto.UpdateReviewRequest;

import java.util.UUID;

public interface ReviewService {

    ReviewResponse submitReview(UUID customerId, CreateReviewRequest request);

    ReviewResponse getReview(UUID reviewId);

    CursorPage<ReviewResponse> getMyReviews(UUID customerId, UUID cursor, int size);

    CursorPage<ReviewResponse> getRestaurantReviews(UUID restaurantId, UUID cursor, int size);

    RestaurantRatingSummaryResponse getRestaurantRatingSummary(UUID restaurantId);

    ReviewResponse updateReview(UUID reviewId, UUID customerId, UpdateReviewRequest request);

    void deleteReview(UUID reviewId, UUID customerId);
}
