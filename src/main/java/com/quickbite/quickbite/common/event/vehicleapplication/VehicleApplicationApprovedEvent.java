package com.quickbite.quickbite.common.event.vehicleapplication;


import java.time.Instant;
import java.util.UUID;

public record VehicleApplicationApprovedEvent(
        UUID applicationId,
        UUID agentUserId,
        UUID deliveryAgentId,
        String vehicleName,
        UUID adminId,
        Instant approvedAt
) implements VehicleApplicationEvent {
}
