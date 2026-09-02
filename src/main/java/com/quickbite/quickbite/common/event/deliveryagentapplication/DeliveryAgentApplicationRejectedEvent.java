package com.quickbite.quickbite.common.event.deliveryagentapplication;

import java.time.Instant;
import java.util.UUID;

public record DeliveryAgentApplicationRejectedEvent(
        UUID applicationId,
        UUID agentUserId,
        UUID adminId,
        String rejectionRemarks,
        Instant rejectedAt
) implements DeliveryAgentApplicationEvent {
}
