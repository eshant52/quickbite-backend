package com.quickbite.quickbite.dtos.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String refreshToken,
        UUID familyId,
        String tokenType
) {
    public AuthResponse {
        tokenType = "Bearer";
    }
}
