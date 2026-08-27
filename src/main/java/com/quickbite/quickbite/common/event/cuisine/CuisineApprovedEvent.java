package com.quickbite.quickbite.common.event.cuisine;

import java.time.Instant;
import java.util.UUID;

public record CuisineApprovedEvent(
        UUID requestId,
        UUID cuisineId,
        String cuisineName,
        UUID requesterId,
        UUID adminId,
        Instant approvedAt
) implements CuisineEvent {}
