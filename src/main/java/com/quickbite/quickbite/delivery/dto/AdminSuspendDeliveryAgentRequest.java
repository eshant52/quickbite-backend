package com.quickbite.quickbite.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminSuspendDeliveryAgentRequest(
        @NotBlank(message = "Suspension reason is required")
        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        String reason
) {}
