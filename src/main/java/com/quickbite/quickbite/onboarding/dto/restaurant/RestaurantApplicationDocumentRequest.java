package com.quickbite.quickbite.onboarding.dto.restaurant;

import com.quickbite.quickbite.restaurant.model.RestaurantDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RestaurantApplicationDocumentRequest(
        @NotNull(message = "Document type is required") RestaurantDocumentType type,
        @NotBlank(message = "Document URL is required") String url
) {}
