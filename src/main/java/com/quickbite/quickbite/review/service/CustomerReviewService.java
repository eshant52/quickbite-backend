package com.quickbite.quickbite.review.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.review.dto.CreateReviewRequest;
import com.quickbite.quickbite.review.dto.ReviewResponse;
import com.quickbite.quickbite.review.dto.UpdateReviewRequest;

import java.util.UUID;

/**
 * Service dedicated to customer feedback submission and review management.
 */
public interface CustomerReviewService {

    ReviewResponse submitReview(UUID customerId, CreateReviewRequest request);

    ReviewResponse getReview(UUID reviewId);

    CursorPage<ReviewResponse> getMyReviews(UUID customerId, UUID cursor, int size);

    ReviewResponse updateReview(UUID reviewId, UUID customerId, UpdateReviewRequest request);

    void deleteReview(UUID reviewId, UUID customerId);
}
