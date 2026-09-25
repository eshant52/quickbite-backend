package com.quickbite.quickbite.common.event.delivery;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.time.Instant;
import java.util.UUID;

@JsonTypeName("DELIVERY_AGENT_ASSIGNED")
public record DeliveryAgentAssignedEvent(
        UUID orderId,
        UUID customerId,
        UUID agentId,
        String agentName,
        String agentPhoneNumber,
        Instant assignedAt
) implements DeliveryEvent {}
