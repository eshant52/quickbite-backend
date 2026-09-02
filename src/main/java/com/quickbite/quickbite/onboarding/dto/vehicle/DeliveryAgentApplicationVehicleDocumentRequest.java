package com.quickbite.quickbite.onboarding.dto.vehicle;

import com.quickbite.quickbite.vehicle.model.VehicleOwnershipDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeliveryAgentApplicationVehicleDocumentRequest(
        @NotNull(message = "Document type is required")
        VehicleOwnershipDocumentType type,

        @NotBlank(message = "Document URL is required")
        String url
) {}
