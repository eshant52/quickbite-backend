package com.quickbite.quickbite.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to {@link QuickBiteTopics#RESTAURANT_REJECTED} when an admin rejects
 * a restaurant onboarding application. The owner can reopen and resubmit.
 */
public record RestaurantApplicationRejectedEvent(
        UUID applicationId,
        UUID ownerId,
        String ownerEmail,
        String ownerName,
        String restaurantName,
        UUID adminId,
        String rejectionRemarks,
        Instant rejectedAt
) {}
