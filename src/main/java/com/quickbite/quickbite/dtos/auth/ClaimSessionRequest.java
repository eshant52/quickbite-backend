package com.quickbite.quickbite.dtos.auth;

import jakarta.validation.constraints.NotBlank;

public record ClaimSessionRequest(
        @NotBlank
        String sessionManagementToken
) {
}
