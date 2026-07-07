package com.quickbite.quickbite.dtos;

import com.quickbite.quickbite.models.User;
import com.quickbite.quickbite.models.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponseDto(
        UUID id,
        String name,
        String email,
        String phoneNumber,
        UserRole role,
        boolean isActive,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponseDto toDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.isActive(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
