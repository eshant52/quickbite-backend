package com.quickbite.quickbite.common.event.deliveryagentapplication;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DeliveryAgentApplicationSubmittedEvent(
        UUID applicationId,
        UUID agentUserId,
        List<UUID> allottedAdminIds,
        Instant submittedAt
) implements DeliveryAgentApplicationEvent {
}
