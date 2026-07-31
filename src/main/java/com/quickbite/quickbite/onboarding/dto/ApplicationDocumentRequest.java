package com.quickbite.quickbite.onboarding.dto;

import com.quickbite.quickbite.restaurant.model.RestaurantDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApplicationDocumentRequest(
        @NotNull(message = "Document type is required") RestaurantDocumentType type,
        @NotBlank(message = "Document URL is required") String url
) {}
