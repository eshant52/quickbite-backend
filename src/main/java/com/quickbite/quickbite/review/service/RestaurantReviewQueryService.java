package com.quickbite.quickbite.review.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.review.dto.RestaurantRatingSummaryResponse;
import com.quickbite.quickbite.review.dto.ReviewResponse;

import java.util.UUID;

/**
 * Read-only catalog query service for public restaurant reviews and aggregate ratings.
 */
public interface RestaurantReviewQueryService {

    CursorPage<ReviewResponse> getRestaurantReviews(UUID restaurantId, UUID cursor, int size);

    RestaurantRatingSummaryResponse getRestaurantRatingSummary(UUID restaurantId);
}
