package com.quickbite.quickbite.menu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record MenuItemRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank @Size(max = 1000) String description,
        @NotNull UUID cuisineId,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotBlank @Size(max = 50) String category,
        boolean isAvailable) {
}
