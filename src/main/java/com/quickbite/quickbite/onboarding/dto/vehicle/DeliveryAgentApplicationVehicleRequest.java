package com.quickbite.quickbite.onboarding.dto.vehicle;

import com.quickbite.quickbite.vehicle.model.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeliveryAgentApplicationVehicleRequest(
        @NotBlank(message = "VIN / Chassis number is required")
        @Size(min = 6, max = 30, message = "VIN must be between 6 and 30 characters")
        String vinNumber,

        @NotBlank(message = "Number plate is required")
        @Size(min = 3, max = 20, message = "Number plate must be between 3 and 20 characters")
        String numberPlate,

        @NotNull(message = "Vehicle type is required")
        VehicleType vehicleType,

        @NotBlank(message = "Brand is required")
        @Size(max = 50)
        String brand,

        @NotBlank(message = "Model is required")
        @Size(max = 50)
        String model,

        boolean isOwnershipTransferred
) {}
