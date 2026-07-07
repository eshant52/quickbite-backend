package com.quickbite.quickbite.dtos.auth;

import java.util.UUID;

public record AuthenticatedSession(
        UUID userId,
        UUID familyId
) {
}
