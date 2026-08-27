package com.quickbite.quickbite.common.event.cuisine;

import java.time.Instant;
import java.util.UUID;

public record CuisineRejectedEvent(
        UUID requestId,
        String cuisineName,
        UUID requesterId,
        UUID adminId,
        String rejectionRemarks,
        Instant rejectedAt
) implements CuisineEvent {}
