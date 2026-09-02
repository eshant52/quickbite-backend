package com.quickbite.quickbite.onboarding.dto.vehicle;

import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.model.vehicle.VehicleApplication;
import com.quickbite.quickbite.vehicle.model.VehicleType;

import java.util.UUID;

/**
 * Returns the vehicle detail fields entered by the agent.
 * Used by GET and PATCH /{id}/details endpoints.
 */
public record VehicleApplicationDetailsResponse(
        UUID id,
        ApplicationStatus status,
        String vinNumber,
        String numberPlate,
        VehicleType vehicleType,
        String brand,
        String model,
        boolean ownershipTransferred,
        UUID existingVehicleId
) {
    public static VehicleApplicationDetailsResponse from(VehicleApplication v) {
        return new VehicleApplicationDetailsResponse(
                v.getId(),
                v.getStatus(),
                v.getVinNumber(),
                v.getNumberPlate(),
                v.getVehicleType(),
                v.getBrand(),
                v.getModel(),
                v.isOwnershipTransferred(),
                v.getExistingVehicle() != null ? v.getExistingVehicle().getId() : null
        );
    }
}
