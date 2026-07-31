package com.quickbite.quickbite.onboarding.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApplicationAddressRequest(
        @NotBlank(message = "Street is required")
        @Size(max = 150)
        String street,

        @NotBlank(message = "City is required")
        @Size(max = 50)
        String city,

        @NotBlank(message = "State is required")
        @Size(max = 50)
        String state,

        @NotBlank(message = "Country is required")
        @Size(max = 50)
        String country,

        @Size(max = 10)
        String postalCode,

        @Size(max = 20)
        String houseNumber,

        @Size(max = 100)
        String buildingName,

        @Size(max = 100)
        String landmark,

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
        Double latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
        Double longitude
) {}
