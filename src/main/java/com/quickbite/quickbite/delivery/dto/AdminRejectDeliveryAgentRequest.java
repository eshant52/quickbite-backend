package com.quickbite.quickbite.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminRejectDeliveryAgentRequest(
        @NotBlank(message = "Rejection remarks are required")
        @Size(max = 500, message = "Remarks cannot exceed 500 characters")
        String remarks
) {}
