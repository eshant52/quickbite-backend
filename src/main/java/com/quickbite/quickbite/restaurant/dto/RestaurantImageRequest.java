package com.quickbite.quickbite.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record RestaurantImageRequest(
        @NotBlank(message = "Image URL is required")
        String imageUrl,

        @PositiveOrZero(message = "Display order must be 0 or positive")
        int displayOrder
) {}
