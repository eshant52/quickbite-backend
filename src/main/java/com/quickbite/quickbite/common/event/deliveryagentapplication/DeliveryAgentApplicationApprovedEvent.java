package com.quickbite.quickbite.common.event.deliveryagentapplication;

import java.time.Instant;
import java.util.UUID;

public record DeliveryAgentApplicationApprovedEvent(
        UUID applicationId,
        UUID deliveryAgentId,
        UUID agentUserId,
        UUID adminId,
        Instant approvedAt
) implements DeliveryAgentApplicationEvent {
}
