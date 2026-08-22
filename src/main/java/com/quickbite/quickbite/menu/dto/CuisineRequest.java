package com.quickbite.quickbite.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating or updating a cuisine.
 *
 * @param name the name of the cuisine, must be between 2 and 100 characters
 */
public record CuisineRequest(
        @NotBlank @Size(min = 2, max = 100) String name
) {
}
