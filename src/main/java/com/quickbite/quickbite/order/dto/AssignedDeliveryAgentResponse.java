package com.quickbite.quickbite.order.dto;

import com.quickbite.quickbite.delivery.model.DeliveryAgent;

import java.time.Instant;
import java.util.UUID;

public record AssignedDeliveryAgentResponse(
        UUID agentId,
        String name,
        String phoneNumber,
        String vehicleType,
        String vehicleModel,
        String vehicleLicensePlate,
        Double currentLatitude,
        Double currentLongitude,
        Instant assignedAt
) {
    public static AssignedDeliveryAgentResponse from(DeliveryAgent agent) {
        if (agent == null) {
            return null;
        }

        String vehicleType = (agent.getCurrentVehicle() != null && agent.getCurrentVehicle().getVehicleType() != null)
                ? agent.getCurrentVehicle().getVehicleType().name()
                : null;
        String vehicleModel = agent.getCurrentVehicle() != null ? agent.getCurrentVehicle().getModel() : null;
        String licensePlate = agent.getCurrentVehicle() != null ? agent.getCurrentVehicle().getNumberPlate() : null;

        Double lat = agent.getLastLocation() != null ? agent.getLastLocation().getY() : null;
        Double lng = agent.getLastLocation() != null ? agent.getLastLocation().getX() : null;

        return new AssignedDeliveryAgentResponse(
                agent.getId(),
                agent.getUser() != null ? agent.getUser().getName() : null,
                agent.getUser() != null ? agent.getUser().getPhoneNumber() : null,
                vehicleType,
                vehicleModel,
                licensePlate,
                lat,
                lng,
                agent.getLastAssignedAt()
        );
    }
}
