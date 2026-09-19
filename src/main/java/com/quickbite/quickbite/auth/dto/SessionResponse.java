package com.quickbite.quickbite.auth.dto;

import com.quickbite.quickbite.auth.model.Session;

import java.time.Duration;
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
) {
    public static SessionResponse from(Session s) {
        return new SessionResponse(
                s.getId(),
                s.getDeviceName(),
                s.getDeviceOS(),
                s.getClientType() != null ? s.getClientType().name() : null,
                s.getIp(),
                s.getLastUsedAt(),
                s.getLoginAt(),
                Math.max(0, (int) Duration.between(Instant.now(), s.getExpiresAt()).toDays())
        );
    }
}
