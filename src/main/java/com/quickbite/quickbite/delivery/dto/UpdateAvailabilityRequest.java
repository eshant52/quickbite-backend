package com.quickbite.quickbite.delivery.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for a delivery agent toggling their working shift availability.
 */
public record UpdateAvailabilityRequest(
        @NotNull(message = "available flag is required")
        Boolean available
) {}
