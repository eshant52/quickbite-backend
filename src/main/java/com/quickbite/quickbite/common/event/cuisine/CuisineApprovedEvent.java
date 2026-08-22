package com.quickbite.quickbite.common.event.cuisine;

import java.time.Instant;
import java.util.UUID;

public record CuisineApprovedEvent(
        UUID cuisineId,
        String cuisineName,
        UUID adminId,
        Instant approvedAt
) {
}
