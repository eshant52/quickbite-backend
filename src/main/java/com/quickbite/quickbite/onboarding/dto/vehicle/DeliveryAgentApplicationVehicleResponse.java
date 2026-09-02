package com.quickbite.quickbite.onboarding.dto.vehicle;

import com.quickbite.quickbite.onboarding.model.vehicle.VehicleApplication;
import com.quickbite.quickbite.vehicle.model.VehicleType;

import java.util.List;
import java.util.UUID;

public record DeliveryAgentApplicationVehicleResponse(
        UUID id,
        String vinNumber,
        String numberPlate,
        VehicleType vehicleType,
        String brand,
        String model,
        boolean isOwnershipTransferred,
        List<DeliveryAgentApplicationVehicleDocumentResponse> documents
) {
    public static DeliveryAgentApplicationVehicleResponse from(VehicleApplication v) {
        if (v == null) return null;
        List<DeliveryAgentApplicationVehicleDocumentResponse> docs = v.getDocuments() != null
                ? v.getDocuments().stream().map(DeliveryAgentApplicationVehicleDocumentResponse::from).toList()
                : List.of();

        return new DeliveryAgentApplicationVehicleResponse(
                v.getId(),
                v.getVinNumber(),
                v.getNumberPlate(),
                v.getVehicleType(),
                v.getBrand(),
                v.getModel(),
                v.isOwnershipTransferred(),
                docs
        );
    }
}
