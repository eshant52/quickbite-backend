package com.quickbite.quickbite.auth.dto;

import java.util.UUID;

public record IssuedToken(
        String rawToken,
        UUID sessionId,
        UUID userId
) {}
