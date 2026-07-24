package com.quickbite.quickbite.auth.dto;

import io.micrometer.core.instrument.util.StringEscapeUtils;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank
        String name,

        @Email
        @NotBlank
        String email,

        @NotBlank(message = "Phone number is required")
        @Size(min = 10, max = 20, message = "Phone number must be between 10 and 20 characters")
        String phoneNumber,

        @NotBlank
        String password
) {
        public RegisterRequest {
                name = StringEscapeUtils.escapeJson(name);
                email = StringEscapeUtils.escapeJson(email);
                phoneNumber = StringEscapeUtils.escapeJson(phoneNumber);
                password = StringEscapeUtils.escapeJson(password);
        }

        public static RegisterRequest xssValidate(RegisterRequest registerRequest) {
                return new RegisterRequest(
                        registerRequest.name(),
                        registerRequest.email(),
                        registerRequest.phoneNumber(),
                        registerRequest.password()
                );
        }
}
