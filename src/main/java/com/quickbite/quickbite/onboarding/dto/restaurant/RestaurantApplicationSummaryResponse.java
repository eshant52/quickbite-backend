package com.quickbite.quickbite.onboarding.dto.restaurant;

import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.model.restaurant.RestaurantApplication;

import java.time.Instant;
import java.util.UUID;

public record RestaurantApplicationSummaryResponse(
        UUID id,
        String ownerName,
        String ownerEmail,
        String restaurantName,
        ApplicationStatus status,
        Instant updatedAt
) {
    public static RestaurantApplicationSummaryResponse from(RestaurantApplication app) {
        return new RestaurantApplicationSummaryResponse(
                app.getId(),
                app.getOwner().getName(),
                app.getOwner().getEmail(),
                app.getName(),
                app.getStatus(),
                app.getUpdatedAt()
        );
    }
}
