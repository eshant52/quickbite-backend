package com.quickbite.quickbite.onboarding.dto.deliveryagent;

import com.quickbite.quickbite.onboarding.dto.vehicle.DeliveryAgentApplicationVehicleResponse;
import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.model.deliveryagent.DeliveryAgentApplication;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DeliveryAgentApplicationResponse(
        UUID id,
        UUID agentId,
        String agentName,
        String agentEmail,
        String agentPhone,
        boolean documentsComplete,
        boolean vehicleComplete,
        ApplicationStatus status,
        UUID reviewedById,
        Instant reviewedAt,
        String rejectionRemarks,
        UUID deliveryAgentId,
        List<DeliveryAgentApplicationDocumentResponse> documents,
        DeliveryAgentApplicationVehicleResponse vehicle,
        Instant createdAt,
        Instant updatedAt
) {
    public static DeliveryAgentApplicationResponse from(DeliveryAgentApplication app) {
        List<DeliveryAgentApplicationDocumentResponse> docs = app.getDocuments() != null
                ? app.getDocuments().stream().map(DeliveryAgentApplicationDocumentResponse::from).toList()
                : List.of();

        DeliveryAgentApplicationVehicleResponse vehicleResponse = (app.getVehicles() != null && !app.getVehicles().isEmpty())
                ? DeliveryAgentApplicationVehicleResponse.from(app.getVehicles().getFirst())
                : null;

        return new DeliveryAgentApplicationResponse(
                app.getId(),
                app.getAgent().getId(),
                app.getAgent().getName(),
                app.getAgent().getEmail(),
                app.getAgent().getPhoneNumber(),
                app.isDocumentsComplete(),
                app.isVehicleComplete(),
                app.getStatus(),
                app.getReviewedBy() != null ? app.getReviewedBy().getId() : null,
                app.getReviewedAt(),
                app.getRejectionRemarks(),
                app.getDeliveryAgent() != null ? app.getDeliveryAgent().getId() : null,
                docs,
                vehicleResponse,
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }
}
