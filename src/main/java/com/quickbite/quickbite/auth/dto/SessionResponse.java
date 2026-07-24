package com.quickbite.quickbite.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID sessionId,
        String deviceName,
        String deviceOS,
        String clientType,
        String ipAddress,
        Instant lastUsedAt,
        Instant loginAt,
        Integer daysLeft
) {}
