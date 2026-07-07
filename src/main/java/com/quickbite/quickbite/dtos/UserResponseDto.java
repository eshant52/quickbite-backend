package com.quickbite.quickbite.dtos;

import com.quickbite.quickbite.models.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
        UUID id,
        String name,
        String email,
        String phoneNumber,
        UserRole role,
        boolean isActive,
        Instant lastLoginAt
) {
}
