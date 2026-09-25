package com.quickbite.quickbite.vehicle.dto;

import com.quickbite.quickbite.vehicle.model.OwnershipStatus;
import com.quickbite.quickbite.vehicle.model.Vehicle;
import com.quickbite.quickbite.vehicle.model.VehicleOwnership;
import com.quickbite.quickbite.vehicle.model.VehicleType;

import java.time.Instant;
import java.util.UUID;

public record VehicleSummaryResponse(
        UUID ownershipId,
        UUID vehicleId,
        String vinNumber,
        String numberPlate,
        VehicleType vehicleType,
        String brand,
        String model,
        OwnershipStatus ownershipStatus,
        boolean isCurrentShiftVehicle,
        Instant createdAt
) {
    public static VehicleSummaryResponse from(VehicleOwnership ownership, UUID currentVehicleId) {
        Vehicle v = ownership.getVehicle();
        boolean isCurrent = currentVehicleId != null
                && v != null
                && currentVehicleId.equals(v.getId())
                && ownership.getCurrentStatus() == OwnershipStatus.ACTIVE;
        return new VehicleSummaryResponse(
                ownership.getId(),
                v.getId(),
                v.getVinNumber(),
                v.getNumberPlate(),
                v.getVehicleType(),
                v.getBrand(),
                v.getModel(),
                ownership.getCurrentStatus(),
                isCurrent,
                ownership.getCreatedAt()
        );
    }
}
