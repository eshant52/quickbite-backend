package com.quickbite.quickbite.restaurant.dto;

import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.model.RestaurantVerificationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RestaurantSummaryResponse(
        UUID id,
        String name,
        BigDecimal avgRating,
        Long totalRating,
        boolean isClosed,
        RestaurantVerificationStatus currentStatus,
        Instant createdAt
) {
    public static RestaurantSummaryResponse from(Restaurant restaurant) {
        return new RestaurantSummaryResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getAvgRating(),
                restaurant.getTotalRating(),
                restaurant.isClosed(),
                restaurant.getCurrentStatus(),
                restaurant.getCreatedAt()
        );
    }
}
