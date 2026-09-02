package com.quickbite.quickbite.onboarding.dto.restaurant;

import com.quickbite.quickbite.onboarding.model.restaurant.RestaurantApplication;

public record RestaurantApplicationAddressResponse(
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
) {
    public static RestaurantApplicationAddressResponse from(RestaurantApplication app) {
        if (app.getAddressStreet() == null) {
            return null;
        }
        Double lat = app.getAddressLocation() != null ? app.getAddressLocation().getY() : null;
        Double lon = app.getAddressLocation() != null ? app.getAddressLocation().getX() : null;
        return new RestaurantApplicationAddressResponse(
                app.getAddressStreet(), app.getAddressCity(), app.getAddressState(),
                app.getAddressCountry(), app.getAddressPostalCode(), app.getAddressHouseNumber(),
                app.getAddressBuildingName(), app.getAddressLandmark(), lat, lon
        );
    }
}
