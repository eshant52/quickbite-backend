package com.quickbite.quickbite.onboarding.dto.vehicle;

import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.model.vehicle.VehicleApplication;
import com.quickbite.quickbite.vehicle.model.VehicleType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VehicleApplicationResponse(
        UUID id,
        UUID deliveryAgentId,
        String agentName,
        String vinNumber,
        String numberPlate,
        VehicleType vehicleType,
        String brand,
        String model,
        boolean isOwnershipTransferred,
        ApplicationStatus status,
        UUID reviewedById,
        Instant reviewedAt,
        String rejectionRemarks,
        List<DeliveryAgentApplicationVehicleDocumentResponse> documents,
        Instant createdAt,
        Instant updatedAt
) {
    public static VehicleApplicationResponse from(VehicleApplication v) {
        List<DeliveryAgentApplicationVehicleDocumentResponse> docs = v.getDocuments() != null
                ? v.getDocuments().stream().map(DeliveryAgentApplicationVehicleDocumentResponse::from).toList()
                : List.of();

        UUID agentId = v.getDeliveryAgent() != null ? v.getDeliveryAgent().getId() : null;
        String agentName = (v.getDeliveryAgent() != null && v.getDeliveryAgent().getUser() != null)
                ? v.getDeliveryAgent().getUser().getName()
                : null;

        return new VehicleApplicationResponse(
                v.getId(),
                agentId,
                agentName,
                v.getVinNumber(),
                v.getNumberPlate(),
                v.getVehicleType(),
                v.getBrand(),
                v.getModel(),
                v.isOwnershipTransferred(),
                v.getStatus(),
                v.getReviewedBy() != null ? v.getReviewedBy().getId() : null,
                v.getReviewedAt(),
                v.getRejectionRemarks(),
                docs,
                v.getCreatedAt(),
                v.getUpdatedAt()
        );
    }
}
