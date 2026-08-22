package com.quickbite.quickbite.user.dto;

import java.util.UUID;

public record AddressResponse(
        UUID id,
        String label,
        String houseNumber,
        String buildingName,
        String street,
        String landmark,
        String city,
        String state,
        String country,
        String postalCode,
        Double latitude,
        Double longitude,
        boolean isDefault
) {
    public static AddressResponse from(com.quickbite.quickbite.user.model.Address address) {
        // JTS convention X = longitude, Y = latitude
        Double lon = address.getLocation() != null ? address.getLocation().getX() : null;
        Double lat = address.getLocation() != null ? address.getLocation().getY() : null;
        return new AddressResponse(
                address.getId(),
                address.getLabel(),
                address.getHouseNumber(),
                address.getBuildingName(),
                address.getStreet(),
                address.getLandmark(),
                address.getCity(),
                address.getState(),
                address.getCountry(),
                address.getPostalCode(),
                lat,
                lon,
                Boolean.TRUE.equals(address.getIsDefault())
        );
    }
}
