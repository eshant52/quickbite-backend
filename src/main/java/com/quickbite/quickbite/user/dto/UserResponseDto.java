package com.quickbite.quickbite.user.dto;

import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.NumberFormat;

import java.time.Instant;
import java.util.UUID;

public record UserResponseDto(
        @org.hibernate.validator.constraints.UUID
        UUID id,

        @NotBlank
        String name,

        @NotBlank
        @Email
        String email,

        @NotBlank
        String phoneNumber,

        @NotBlank
        UserRole role,

        @NotBlank
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
