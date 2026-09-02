package com.quickbite.quickbite.review.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.review.dto.ReviewResponse;

import java.util.UUID;

public interface AdminReviewService {

    CursorPage<ReviewResponse> listAllReviews(UUID restaurantId, UUID cursor, int size);

    ReviewResponse getReviewAsAdmin(UUID reviewId);

    void deleteReviewAsAdmin(UUID reviewId);
}
