package com.quickbite.quickbite.restaurant.dto;

import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.model.RestaurantVerificationStatus;
import com.quickbite.quickbite.user.dto.AddressResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RestaurantResponse(
        UUID id,
        String name,
        String description,
        BigDecimal avgRating,
        Long totalRating,
        boolean isClosed,
        RestaurantVerificationStatus currentStatus,
        UUID ownerId,
        AddressResponse address,
        List<RestaurantHoursResponse> hours,
        List<RestaurantImageResponse> images,
        Instant createdAt) {
    public static RestaurantResponse from(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getAvgRating(),
                restaurant.getTotalRating(),
                restaurant.isClosed(),
                restaurant.getCurrentStatus(),
                restaurant.getOwner().getId(),
                AddressResponse.from(restaurant.getAddress()),
                restaurant.getRestaurantHours() != null
                        ? restaurant.getRestaurantHours().stream().map(RestaurantHoursResponse::from).toList()
                        : List.of(),
                restaurant.getRestaurantImages() != null
                        ? restaurant.getRestaurantImages().stream().map(RestaurantImageResponse::from).toList()
                        : List.of(),
                restaurant.getCreatedAt()
        );
    }
}
