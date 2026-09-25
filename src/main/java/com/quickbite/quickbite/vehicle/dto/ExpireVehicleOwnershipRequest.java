package com.quickbite.quickbite.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExpireVehicleOwnershipRequest(
        @NotBlank(message = "Remarks are required when expiring a vehicle ownership")
        @Size(max = 500, message = "Remarks must be at most 500 characters")
        String remarks
) {}
