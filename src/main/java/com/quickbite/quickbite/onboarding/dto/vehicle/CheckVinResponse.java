package com.quickbite.quickbite.onboarding.dto.vehicle;

import com.quickbite.quickbite.vehicle.model.VehicleType;

import java.util.UUID;

public record CheckVinResponse(
        boolean exists,
        UUID vehicleId,
        String vinNumber,
        String numberPlate,
        String brand,
        String model,
        VehicleType vehicleType
) {
    public static CheckVinResponse notFound(String vinNumber) {
        return new CheckVinResponse(false, null, vinNumber, null, null, null, null);
    }
}
