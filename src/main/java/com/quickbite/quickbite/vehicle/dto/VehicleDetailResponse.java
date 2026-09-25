package com.quickbite.quickbite.vehicle.dto;

import com.quickbite.quickbite.vehicle.model.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VehicleDetailResponse(
        UUID ownershipId,
        UUID vehicleId,
        UUID ownerDeliveryAgentId,
        String vinNumber,
        String numberPlate,
        VehicleType vehicleType,
        String brand,
        String model,
        OwnershipStatus ownershipStatus,
        boolean isCurrentShiftVehicle,
        List<VehicleOwnershipDocumentDto> documents,
        List<VehicleOwnershipStatusHistoryDto> statusHistory,
        Instant createdAt,
        Instant updatedAt
) {
    public static VehicleDetailResponse from(
            VehicleOwnership ownership,
            UUID currentVehicleId,
            List<VehicleOwnershipDocument> documents,
            List<VehicleOwnershipStatusHistory> history
    ) {
        Vehicle v = ownership.getVehicle();
        boolean isCurrent = currentVehicleId != null
                && v != null
                && currentVehicleId.equals(v.getId())
                && ownership.getCurrentStatus() == OwnershipStatus.ACTIVE;
        assert v != null;
        return new VehicleDetailResponse(
                ownership.getId(),
                v.getId(),
                ownership.getOwner() != null ? ownership.getOwner().getId() : null,
                v.getVinNumber(),
                v.getNumberPlate(),
                v.getVehicleType(),
                v.getBrand(),
                v.getModel(),
                ownership.getCurrentStatus(),
                isCurrent,
                documents.stream().map(VehicleOwnershipDocumentDto::from).toList(),
                history.stream().map(VehicleOwnershipStatusHistoryDto::from).toList(),
                ownership.getCreatedAt(),
                ownership.getUpdatedAt()
        );
    }
}
