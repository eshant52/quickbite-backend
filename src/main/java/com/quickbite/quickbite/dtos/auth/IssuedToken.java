package com.quickbite.quickbite.dtos.auth;

import com.quickbite.quickbite.models.User;

import java.util.UUID;

public record IssuedToken(
        String rawToken,
        UUID familyId,
        UUID userId
) {
}
