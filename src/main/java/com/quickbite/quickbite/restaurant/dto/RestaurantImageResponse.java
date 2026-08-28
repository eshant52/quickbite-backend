package com.quickbite.quickbite.restaurant.dto;

import com.quickbite.quickbite.restaurant.model.RestaurantImage;

import java.util.UUID;

public record RestaurantImageResponse(
        UUID id,
        String imageUrl,
        int displayOrder) {

    public static RestaurantImageResponse from(RestaurantImage restaurantImage) {
        return new RestaurantImageResponse(
                restaurantImage.getId(),
                restaurantImage.getImageUrl(),
                restaurantImage.getDisplayOrder()
        );
    }
}
