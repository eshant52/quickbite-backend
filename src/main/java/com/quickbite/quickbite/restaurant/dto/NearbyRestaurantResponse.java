package com.quickbite.quickbite.restaurant.dto;

import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.model.RestaurantVerificationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Summary response for a restaurant returned in a nearby search.
 * Extends the base summary with the computed straight-line distance
 * from the customer's query location.
 */
public record NearbyRestaurantResponse(
        UUID id,
        String name,
        BigDecimal avgRating,
        Long totalRating,
        boolean isClosed,
        RestaurantVerificationStatus currentStatus,
        Instant createdAt,
        /* Straight-line distance from the customer's query location in metres. */
        double distanceMeters
) {
    public static NearbyRestaurantResponse from(Restaurant restaurant, double distanceMeters) {
        return new NearbyRestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getAvgRating(),
                restaurant.getTotalRating(),
                restaurant.isClosed(),
                restaurant.getCurrentStatus(),
                restaurant.getCreatedAt(),
                distanceMeters
        );
    }
}
