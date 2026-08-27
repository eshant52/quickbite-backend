package com.quickbite.quickbite.menu.dto;

import com.quickbite.quickbite.menu.model.Cuisine;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing an approved cuisine in the master platform catalog.
 */
public record CuisineResponse(
        UUID id,
        String name,
        Instant createdAt
) {
    public static CuisineResponse from(Cuisine cuisine) {
        return new CuisineResponse(
                cuisine.getId(),
                cuisine.getName(),
                cuisine.getCreatedAt()
        );
    }
}
