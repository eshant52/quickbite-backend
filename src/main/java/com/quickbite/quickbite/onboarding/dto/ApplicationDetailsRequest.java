package com.quickbite.quickbite.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplicationDetailsRequest(
        @NotBlank(message = "Restaurant name is required")
        @Size(min = 2, max = 200, message = "Restaurant name must be between 2 and 200 characters")
        String name,

        @NotBlank(message = "Description is required")
        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description
) {}
