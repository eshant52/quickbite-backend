package com.quickbite.quickbite.review.dto;

import com.quickbite.quickbite.review.model.Review;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID restaurantId,
        String restaurantName,
        UUID customerId,
        String customerName,
        UUID orderId,
        int rating,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getRestaurant().getId(),
                review.getRestaurant().getName(),
                review.getCustomer().getId(),
                review.getCustomer().getName(),
                review.getOrder().getId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
