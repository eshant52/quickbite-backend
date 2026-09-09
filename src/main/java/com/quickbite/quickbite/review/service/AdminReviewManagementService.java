package com.quickbite.quickbite.review.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.review.dto.ReviewResponse;

import java.util.UUID;

/**
 * Administrative service for platform-wide review moderation and oversight.
 */
public interface AdminReviewManagementService {

    CursorPage<ReviewResponse> listReviews(UUID restaurantId, UUID cursor, int size);

    ReviewResponse getReview(UUID reviewId);

    void deleteReview(UUID reviewId);
}
