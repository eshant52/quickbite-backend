package com.quickbite.quickbite.user.dto;

import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String name,
        String phoneNumber,
        String email,
        UserRole role,
        boolean isActive,
        Instant createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
