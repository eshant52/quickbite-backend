package com.quickbite.quickbite.common.event.cuisine;

import java.time.Instant;
import java.util.UUID;

public record CuisineRequestedEvent(
        UUID cuisineId,
        String cuisineName,
        UUID requesterId,
        Instant requestedAt
) {
}
