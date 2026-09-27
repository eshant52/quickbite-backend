package com.quickbite.quickbite.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminRefundOrderRequest(
        @NotBlank(message = "Refund reason is required")
        @Size(max = 500, message = "Refund reason must not exceed 500 characters")
        String reason
) {
}
