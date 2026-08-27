package com.quickbite.quickbite.allotment.dto;

import com.quickbite.quickbite.allotment.model.AdminAllotment;
import com.quickbite.quickbite.allotment.model.AllotmentReferenceType;
import com.quickbite.quickbite.allotment.model.AllotmentStatus;

import java.time.Instant;
import java.util.UUID;

public record AllotmentResponse(
        UUID id,
        UUID referenceId,
        AllotmentReferenceType referenceType,
        AllotmentStatus status,
        Instant notifiedAt,
        Instant respondedAt
) {
    public static AllotmentResponse from(AdminAllotment allotment) {
        return new AllotmentResponse(
                allotment.getId(),
                allotment.getReferenceId(),
                allotment.getReferenceType(),
                allotment.getStatus(),
                allotment.getNotifiedAt(),
                allotment.getRespondedAt()
        );
    }
}
