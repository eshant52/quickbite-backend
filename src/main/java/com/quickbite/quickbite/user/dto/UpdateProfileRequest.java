package com.quickbite.quickbite.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank @Size(min = 10, max = 15) String phoneNumber,
        @NotBlank @Email(message = "Email should be valid") String email
) {
}
