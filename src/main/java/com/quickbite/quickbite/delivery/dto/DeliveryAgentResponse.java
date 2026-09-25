package com.quickbite.quickbite.delivery.dto;

import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.model.DeliveryAgentVerificationStatus;
import com.quickbite.quickbite.vehicle.model.Vehicle;
import com.quickbite.quickbite.vehicle.model.VehicleType;

import java.time.Instant;
import java.util.UUID;

public record DeliveryAgentResponse(
        UUID id,
        UUID userId,
        String userName,
        String userEmail,
        String userPhone,
        boolean isAvailable,
        boolean isAssigned,
        DeliveryAgentVerificationStatus currentStatus,
        UUID currentVehicleId,
        String currentVehicleNumberPlate,
        VehicleType currentVehicleType,
        Double latitude,
        Double longitude,
        Instant createdAt
) {
    /**
     * Backwards-compatible constructor for callers omitting currentVehicle fields.
     */
    public DeliveryAgentResponse(
            UUID id,
            UUID userId,
            String userName,
            String userEmail,
            String userPhone,
            boolean isAvailable,
            boolean isAssigned,
            DeliveryAgentVerificationStatus currentStatus,
            Double latitude,
            Double longitude,
            Instant createdAt
    ) {
        this(id, userId, userName, userEmail, userPhone, isAvailable, isAssigned,
                currentStatus, null, null, null, latitude, longitude, createdAt);
    }

    /**
     * Backwards-compatible constructor for callers omitting isAssigned and currentVehicle fields.
     */
    public DeliveryAgentResponse(
            UUID id,
            UUID userId,
            String userName,
            String userEmail,
            String userPhone,
            boolean isAvailable,
            DeliveryAgentVerificationStatus currentStatus,
            Double latitude,
            Double longitude,
            Instant createdAt
    ) {
        this(id, userId, userName, userEmail, userPhone, isAvailable, false,
                currentStatus, null, null, null, latitude, longitude, createdAt);
    }

    public static DeliveryAgentResponse from(DeliveryAgent agent) {
        Vehicle currentVehicle = agent.getCurrentVehicle();
        return new DeliveryAgentResponse(
                agent.getId(),
                agent.getUser().getId(),
                agent.getUser().getName(),
                agent.getUser().getEmail(),
                agent.getUser().getPhoneNumber(),
                agent.isAvailable(),
                agent.isAssigned(),
                agent.getCurrentStatus(),
                currentVehicle != null ? currentVehicle.getId() : null,
                currentVehicle != null ? currentVehicle.getNumberPlate() : null,
                currentVehicle != null ? currentVehicle.getVehicleType() : null,
                agent.getLastLocation() != null ? agent.getLastLocation().getY() : null,
                agent.getLastLocation() != null ? agent.getLastLocation().getX() : null,
                agent.getCreatedAt()
        );
    }
}
