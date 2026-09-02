package com.quickbite.quickbite.onboarding.dto.deliveryagent;

import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.model.deliveryagent.DeliveryAgentApplication;

import java.time.Instant;
import java.util.UUID;

public record DeliveryAgentApplicationSummaryResponse(
        UUID id,
        UUID agentId,
        String agentName,
        String agentEmail,
        ApplicationStatus status,
        boolean documentsComplete,
        boolean vehicleComplete,
        Instant createdAt
) {
    public static DeliveryAgentApplicationSummaryResponse from(DeliveryAgentApplication app) {
        return new DeliveryAgentApplicationSummaryResponse(
                app.getId(),
                app.getAgent().getId(),
                app.getAgent().getName(),
                app.getAgent().getEmail(),
                app.getStatus(),
                app.isDocumentsComplete(),
                app.isVehicleComplete(),
                app.getCreatedAt()
        );
    }
}
