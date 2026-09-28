package com.quickbite.quickbite.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAddressRequest(
        @NotBlank @Size(max = 50) String label,
        @Size(max = 20) String houseNumber,
        @Size(max = 100) String buildingName,
        @NotBlank @Size(max = 150) String street,
        @Size(max = 100) String landmark,
        @NotBlank @Size(max = 50) String city,
        @NotBlank @Size(max = 50) String state,
        @NotBlank @Size(max = 50) String country,
        @Size(max = 10) String postalCode,

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
        Double latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
        Double longitude,

        boolean isDefault
) {
}
