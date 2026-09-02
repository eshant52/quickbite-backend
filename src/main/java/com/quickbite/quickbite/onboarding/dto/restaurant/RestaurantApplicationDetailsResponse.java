package com.quickbite.quickbite.onboarding.dto.restaurant;

import com.quickbite.quickbite.onboarding.model.restaurant.RestaurantApplication;

public record RestaurantApplicationDetailsResponse(
        String name,
        String description
) {
    public static RestaurantApplicationDetailsResponse from(RestaurantApplication app) {
        return new RestaurantApplicationDetailsResponse(app.getName(), app.getDescription());
    }
}
