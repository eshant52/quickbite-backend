package com.quickbite.quickbite.menu.dto;

import com.quickbite.quickbite.menu.model.Cuisine;
import com.quickbite.quickbite.menu.model.CuisineStatus;

import java.util.UUID;

/**
 * DTO for Cuisine response.
 *
 * @param id the unique identifier of the cuisine
 * @param name the name of the cuisine
 * @param status the status of the cuisine (e.g., PENDING, APPROVED, REJECTED)
 */
public record CuisineResponse(
        UUID id,
        String name,
        CuisineStatus status
) {

    /**
     * Converts a Cuisine entity to a CuisineResponse DTO.
     *
     * @param cuisine the Cuisine entity to convert
     * @return the corresponding CuisineResponse DTO
     */
    public static CuisineResponse from(Cuisine cuisine) {
        return new CuisineResponse(
                cuisine.getId(),
                cuisine.getName(),
                cuisine.getStatus()
        );
    }
}
