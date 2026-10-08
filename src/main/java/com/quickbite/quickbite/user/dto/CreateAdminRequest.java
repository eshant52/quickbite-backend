package com.quickbite.quickbite.user.dto;

import io.micrometer.core.instrument.util.StringEscapeUtils;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAdminRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Phone number is required")
        @Size(min = 10, max = 20, message = "Phone number must be between 10 and 20 characters")
        String phoneNumber,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
    public CreateAdminRequest {
        name = name != null ? StringEscapeUtils.escapeJson(name.trim()) : null;
        email = email != null ? StringEscapeUtils.escapeJson(email.trim().toLowerCase()) : null;
        phoneNumber = phoneNumber != null ? StringEscapeUtils.escapeJson(phoneNumber.trim()) : null;
        password = password != null ? StringEscapeUtils.escapeJson(password) : null;
    }

    public static CreateAdminRequest xssValidate(CreateAdminRequest request) {
        return new CreateAdminRequest(
                request.name(),
                request.email(),
                request.phoneNumber(),
                request.password()
        );
    }
}
