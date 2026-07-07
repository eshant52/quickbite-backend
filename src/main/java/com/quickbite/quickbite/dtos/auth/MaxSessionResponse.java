package com.quickbite.quickbite.dtos.auth;

public record MaxSessionResponse(
        String sessionManagementToken,
        String message,
        int maxSessions
) {
}
