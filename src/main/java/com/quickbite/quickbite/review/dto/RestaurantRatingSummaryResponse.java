package com.quickbite.quickbite.review.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record RestaurantRatingSummaryResponse(
        UUID restaurantId,
        BigDecimal avgRating,
        long totalRating,
        Map<Integer, Long> ratingDistribution
) {
}
