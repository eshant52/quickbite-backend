package com.quickbite.quickbite.vehicle.dto;

import com.quickbite.quickbite.vehicle.model.OwnershipStatus;
import com.quickbite.quickbite.vehicle.model.VehicleOwnershipStatusHistory;

import java.time.Instant;
import java.util.UUID;

public record VehicleOwnershipStatusHistoryDto(
        UUID id,
        OwnershipStatus status,
        UUID reviewedByUserId,
        String remarks,
        Instant createdAt
) {
    public static VehicleOwnershipStatusHistoryDto from(VehicleOwnershipStatusHistory history) {
        return new VehicleOwnershipStatusHistoryDto(
                history.getId(),
                history.getStatus(),
                history.getReviewedBy() != null ? history.getReviewedBy().getId() : null,
                history.getRemarks(),
                history.getCreatedAt()
        );
    }
}
