package com.quickbite.quickbite.onboarding.dto.vehicle;

import com.quickbite.quickbite.onboarding.model.vehicle.VehicleApplicationDocument;
import com.quickbite.quickbite.vehicle.model.VehicleOwnershipDocumentType;

import java.util.UUID;

public record VehicleApplicationDocumentResponse(
        UUID id,
        VehicleOwnershipDocumentType type,
        String url
) {
    public static VehicleApplicationDocumentResponse from(VehicleApplicationDocument doc) {
        return new VehicleApplicationDocumentResponse(
                doc.getId(),
                doc.getType(),
                doc.getUrl()
        );
    }
}
