package com.quickbite.quickbite.delivery.dto;

import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.model.DeliveryAgentVerificationStatus;

import java.time.Instant;
import java.util.UUID;

public record DeliveryAgentResponse(
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
    public static DeliveryAgentResponse from(DeliveryAgent agent) {
        return new DeliveryAgentResponse(
                agent.getId(),
                agent.getUser().getId(),
                agent.getUser().getName(),
                agent.getUser().getEmail(),
                agent.getUser().getPhoneNumber(),
                agent.isAvailable(),
                agent.getCurrentStatus(),
                agent.getLastLocation() != null ? agent.getLastLocation().getY() : null,
                agent.getLastLocation() != null ? agent.getLastLocation().getX() : null,
                agent.getCreatedAt()
        );
    }
}
