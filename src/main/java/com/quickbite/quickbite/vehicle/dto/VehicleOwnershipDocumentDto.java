package com.quickbite.quickbite.vehicle.dto;

import com.quickbite.quickbite.common.model.DocumentVerificationStatus;
import com.quickbite.quickbite.vehicle.model.VehicleOwnershipDocument;
import com.quickbite.quickbite.vehicle.model.VehicleOwnershipDocumentType;

import java.time.Instant;
import java.util.UUID;

public record VehicleOwnershipDocumentDto(
        UUID id,
        VehicleOwnershipDocumentType type,
        String url,
        String description,
        DocumentVerificationStatus status,
        Instant reviewedAt,
        String remarks
) {
    public static VehicleOwnershipDocumentDto from(VehicleOwnershipDocument doc) {
        return new VehicleOwnershipDocumentDto(
                doc.getId(),
                doc.getType(),
                doc.getUrl(),
                doc.getDescription(),
                doc.getStatus(),
                doc.getReviewedAt(),
                doc.getRemarks()
        );
    }
}
