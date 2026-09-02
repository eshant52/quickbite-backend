package com.quickbite.quickbite.onboarding.dto.restaurant;

import com.quickbite.quickbite.onboarding.model.restaurant.RestaurantApplicationDocument;
import com.quickbite.quickbite.restaurant.model.RestaurantDocumentType;

import java.util.UUID;

public record RestaurantApplicationDocumentResponse(
        UUID id,
        RestaurantDocumentType type,
        String url
) {
    public static RestaurantApplicationDocumentResponse from(RestaurantApplicationDocument doc) {
        return new RestaurantApplicationDocumentResponse(doc.getId(), doc.getType(), doc.getUrl());
    }
}
