package com.quickbite.quickbite.onboarding.dto;

import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.model.RestaurantApplication;

import java.time.Instant;
import java.util.UUID;

public record ApplicationSummaryResponse(
        UUID id,
        String ownerName,
        String ownerEmail,
        String restaurantName,
        ApplicationStatus status,
        Instant updatedAt
) {
    public static ApplicationSummaryResponse from(RestaurantApplication app) {
        return new ApplicationSummaryResponse(
                app.getId(),
                app.getOwner().getName(),
                app.getOwner().getEmail(),
                app.getName(),
                app.getStatus(),
                app.getUpdatedAt()
        );
    }
}
