package com.quickbite.quickbite.onboarding.dto.restaurant;

import com.quickbite.quickbite.onboarding.model.restaurant.RestaurantApplicationImage;

import java.util.UUID;

public record RestaurantApplicationImageResponse(
        UUID id,
        String imageUrl,
        int displayOrder
) {
    public static RestaurantApplicationImageResponse from(RestaurantApplicationImage img) {
        return new RestaurantApplicationImageResponse(img.getId(), img.getImageUrl(), img.getDisplayOrder());
    }
}
