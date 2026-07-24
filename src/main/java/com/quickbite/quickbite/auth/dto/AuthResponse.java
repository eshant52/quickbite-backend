package com.quickbite.quickbite.auth.dto;


import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UUID sessionId,
        String tokenType
) {
    public AuthResponse(String accessToken, String refreshToken, long expiresIn, UUID sessionId) {
        this(accessToken, refreshToken, expiresIn, sessionId, "Bearer");
    }
}
