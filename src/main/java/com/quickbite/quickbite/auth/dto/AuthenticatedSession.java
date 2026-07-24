package com.quickbite.quickbite.auth.dto;

import java.util.UUID;

public record AuthenticatedSession(
        UUID userId,
        UUID sessionId
) {}
