package com.quickbite.quickbite.dtos.auth;

import java.time.Instant;
import java.util.UUID;

public record SessionResponseDto(
        UUID familyId,
        String deviceName,
        String os,
        String clientType,
        Instant createdAt,
        Instant lastUsedAt,
        boolean current
) {
}
