package com.quickbite.quickbite.user.dto;

import jakarta.validation.constraints.NotBlank;
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
        Double latitude,
        Double longitude,
        boolean isDefault
) {
}
