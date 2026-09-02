package com.quickbite.quickbite.onboarding.dto.vehicle;

import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.model.vehicle.VehicleApplication;
import com.quickbite.quickbite.vehicle.model.VehicleType;

import java.time.Instant;
import java.util.UUID;

public record VehicleApplicationSummaryResponse(
        UUID id,
        UUID deliveryAgentId,
        String agentName,
        String vinNumber,
        String numberPlate,
        VehicleType vehicleType,
        String brand,
        String model,
        ApplicationStatus status,
        Instant createdAt
) {
    public static VehicleApplicationSummaryResponse from(VehicleApplication v) {
        UUID agentId = v.getDeliveryAgent() != null ? v.getDeliveryAgent().getId() : null;
        String agentName = (v.getDeliveryAgent() != null && v.getDeliveryAgent().getUser() != null)
                ? v.getDeliveryAgent().getUser().getName()
                : null;

        return new VehicleApplicationSummaryResponse(
                v.getId(),
                agentId,
                agentName,
                v.getVinNumber(),
                v.getNumberPlate(),
                v.getVehicleType(),
                v.getBrand(),
                v.getModel(),
                v.getStatus(),
                v.getCreatedAt()
        );
    }
}
