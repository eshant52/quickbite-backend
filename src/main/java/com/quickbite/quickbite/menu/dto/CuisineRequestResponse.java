package com.quickbite.quickbite.menu.dto;

import com.quickbite.quickbite.menu.model.CuisineRequest;
import com.quickbite.quickbite.menu.model.CuisineStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a cuisine request submitted by a restaurant owner for admin review.
 */
public record CuisineRequestResponse(
        UUID id,
        String name,
        CuisineStatus status,
        UUID requestedById,
        String requestedByName,
        UUID reviewedById,
        Instant reviewedAt,
        String remarks,
        UUID cuisineId,
        Instant createdAt
) {
    public static CuisineRequestResponse from(CuisineRequest request) {
        return new CuisineRequestResponse(
                request.getId(),
                request.getName(),
                request.getStatus(),
                request.getRequestedBy() != null ? request.getRequestedBy().getId() : null,
                request.getRequestedBy() != null ? request.getRequestedBy().getName() : null,
                request.getReviewedBy() != null ? request.getReviewedBy().getId() : null,
                request.getReviewedAt(),
                request.getRemarks(),
                request.getCuisine() != null ? request.getCuisine().getId() : null,
                request.getCreatedAt()
        );
    }
}
