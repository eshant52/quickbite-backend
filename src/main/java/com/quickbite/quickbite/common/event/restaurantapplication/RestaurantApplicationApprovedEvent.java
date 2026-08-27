package com.quickbite.quickbite.common.event.restaurantapplication;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when an admin approves a restaurant onboarding application
 * and the Restaurant entity is created.
 */
public record RestaurantApplicationApprovedEvent(
        UUID applicationId,
        UUID restaurantId,
        UUID ownerId,
        String ownerEmail,
        String ownerName,
        String restaurantName,
        UUID adminId,
        Instant approvedAt
) implements RestaurantApplicationEvent {}
