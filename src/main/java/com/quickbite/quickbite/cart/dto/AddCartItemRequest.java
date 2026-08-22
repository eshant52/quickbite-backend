package com.quickbite.quickbite.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddCartItemRequest(
        @NotNull UUID menuItemId,
        @Min(1) @Max(99) int quantity
) {
}
