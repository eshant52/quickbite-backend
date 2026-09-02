package com.quickbite.quickbite.common.event.vehicleapplication;

import java.time.Instant;
import java.util.UUID;

public record VehicleApplicationRejectedEvent(
        UUID applicationId,
        UUID agentUserId,
        UUID deliveryAgentId,
        String vehicleName,
        String rejectionReason,
        UUID adminId,
        Instant rejectedAt
) implements VehicleApplicationEvent {
}
