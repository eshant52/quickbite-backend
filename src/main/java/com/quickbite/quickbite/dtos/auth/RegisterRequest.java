package com.quickbite.quickbite.dtos.auth;

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
        String password
) {
}
