package com.quickbite.quickbite.auth.dto;


public record SessionLimitErrorResponse(
        String error,
        String challengeToken,
        int maxSessions
) {}
