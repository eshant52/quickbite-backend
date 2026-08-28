package com.quickbite.quickbite.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRestaurantRequest(
        @NotBlank @Size(min = 2, max = 200) String name,
        @NotBlank @Size(max = 2000) String description
) {
}
