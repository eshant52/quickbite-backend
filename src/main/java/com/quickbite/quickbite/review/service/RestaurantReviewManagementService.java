package com.quickbite.quickbite.review.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.review.dto.ReviewResponse;

import java.util.UUID;

/**
 * Domain service for restaurant review moderation and platform oversight.
 */
public interface RestaurantReviewManagementService {

    CursorPage<ReviewResponse> listReviews(UUID restaurantId, UUID cursor, int size);

    ReviewResponse getReview(UUID reviewId);

    void deleteReview(UUID reviewId);
}
