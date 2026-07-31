package com.quickbite.quickbite.onboarding.dto;

import com.quickbite.quickbite.onboarding.model.ApplicationDocument;
import com.quickbite.quickbite.restaurant.model.RestaurantDocumentType;

import java.util.UUID;

public record ApplicationDocumentResponse(
        UUID id,
        RestaurantDocumentType type,
        String url
) {
    public static ApplicationDocumentResponse from(ApplicationDocument doc) {
        return new ApplicationDocumentResponse(doc.getId(), doc.getType(), doc.getUrl());
    }
}
