package com.quickbite.quickbite.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to {@link QuickBiteTopics#RESTAURANT_APPLICATION_SUBMITTED} when an
 * owner submits their onboarding application for admin review.
 */
public record RestaurantApplicationSubmittedEvent(
        UUID applicationId,
        UUID ownerId,
        String ownerEmail,
        String ownerName,
        String restaurantName,
        Instant submittedAt
) {}
