package com.quickbite.quickbite.common.event.vehicleapplication;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VehicleApplicationSubmittedEvent(
        UUID applicationId,
        UUID agentUserId,
        UUID deliveryAgentId,
        String vehicleName,
        List<UUID> allottedAdminIds,
        Instant submittedAt
) implements VehicleApplicationEvent {
}
