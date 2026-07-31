package com.quickbite.quickbite.onboarding.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ApplicationImageRequest(
        @NotBlank(message = "Image URL is required") String imageUrl,
        @Min(0) int displayOrder
) {}
