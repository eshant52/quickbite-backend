package com.quickbite.quickbite.menu.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MenuItemImageRequest(
        @NotBlank String imageUrl,
        @Min(0) int displayOrder) {
}
