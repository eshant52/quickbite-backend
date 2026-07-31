package com.quickbite.quickbite.onboarding.dto;

public record ApplicationAddressResponse(
        String street,
        String city,
        String state,
        String country,
        String postalCode,
        String houseNumber,
        String buildingName,
        String landmark,
        Double latitude,
        Double longitude
) {}
