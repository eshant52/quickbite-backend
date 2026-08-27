package com.quickbite.quickbite.common.event.cuisine;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CuisineRequestedEvent(
        UUID requestId,
        String cuisineName,
        UUID requesterId,
        List<UUID> allottedAdminIds,
        Instant requestedAt
) implements CuisineEvent {}
