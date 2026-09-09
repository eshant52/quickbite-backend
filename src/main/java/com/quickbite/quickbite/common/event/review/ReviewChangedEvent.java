package com.quickbite.quickbite.common.event.review;

import java.util.UUID;

/**
 * Domain event published whenever a review is submitted, updated, or deleted.
 * Consumed by rating listeners to recalculate restaurant rating aggregates.
 */
public record ReviewChangedEvent(UUID restaurantId) {}
