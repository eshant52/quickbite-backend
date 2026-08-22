package com.quickbite.quickbite.common.event.restaurantapplication;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when an owner submits their onboarding application for admin review.
 */
public record RestaurantApplicationSubmittedEvent(
        UUID applicationId,
        UUID ownerId,
        String ownerEmail,
        String ownerName,
        String restaurantName,
        Instant submittedAt
) {}
