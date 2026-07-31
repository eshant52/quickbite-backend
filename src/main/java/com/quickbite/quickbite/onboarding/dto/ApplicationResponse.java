package com.quickbite.quickbite.onboarding.dto;

import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.model.RestaurantApplication;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        ApplicationStatus status,
        String name,
        String description,
        ApplicationAddressResponse address,
        boolean detailsComplete,
        boolean addressComplete,
        boolean hoursComplete,
        boolean imagesComplete,
        boolean documentsComplete,
        List<ApplicationHoursResponse> hours,
        List<ApplicationImageResponse> images,
        List<ApplicationDocumentResponse> documents,
        String rejectionRemarks,
        UUID restaurantId,
        Instant createdAt,
        Instant updatedAt
) {
    public static ApplicationResponse from(RestaurantApplication app) {
        ApplicationAddressResponse address = null;
        if (app.getAddressStreet() != null) {
            Double lat = app.getAddressLocation() != null ? app.getAddressLocation().getY() : null;
            Double lon = app.getAddressLocation() != null ? app.getAddressLocation().getX() : null;
            address = new ApplicationAddressResponse(
                    app.getAddressStreet(), app.getAddressCity(), app.getAddressState(),
                    app.getAddressCountry(), app.getAddressPostalCode(), app.getAddressHouseNumber(),
                    app.getAddressBuildingName(), app.getAddressLandmark(), lat, lon
            );
        }
        return new ApplicationResponse(
                app.getId(),
                app.getStatus(),
                app.getName(),
                app.getDescription(),
                address,
                app.isDetailsComplete(),
                app.isAddressComplete(),
                app.isHoursComplete(),
                app.isImagesComplete(),
                app.isDocumentsComplete(),
                app.getHours().stream().map(ApplicationHoursResponse::from).toList(),
                app.getImages().stream().map(ApplicationImageResponse::from).toList(),
                app.getDocuments().stream().map(ApplicationDocumentResponse::from).toList(),
                app.getRejectionRemarks(),
                app.getRestaurant() != null ? app.getRestaurant().getId() : null,
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }
}
