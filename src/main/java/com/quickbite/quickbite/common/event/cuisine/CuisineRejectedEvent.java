package com.quickbite.quickbite.common.event.cuisine;

import java.time.Instant;
import java.util.UUID;

public record CuisineRejectedEvent(
        UUID cuisineId,
        String cuisineName,
        UUID adminId,
        String rejectionRemarks,
        Instant rejectedAt
) {
}
