package com.quickbite.quickbite.onboarding.dto;

import com.quickbite.quickbite.onboarding.model.ApplicationImage;

import java.util.UUID;

public record ApplicationImageResponse(
        UUID id,
        String imageUrl,
        int displayOrder
) {
    public static ApplicationImageResponse from(ApplicationImage img) {
        return new ApplicationImageResponse(img.getId(), img.getImageUrl(), img.getDisplayOrder());
    }
}
