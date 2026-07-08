package com.quickbite.quickbite.dtos.auth;

import com.quickbite.quickbite.models.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank
        String name,

        @Email
        @NotBlank
        String email,

        @NotBlank
        String phoneNumber,

        @NotBlank
        String password,

        // Optional: desired role. If omitted defaults to CUSTOMER. Self-registration for privileged roles is not allowed.
        UserRole role
) {
}
