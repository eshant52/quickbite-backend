package com.quickbite.quickbite.onboarding.dto.restaurant;

import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.model.restaurant.RestaurantApplication;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RestaurantApplicationResponse(
        UUID id,
        ApplicationStatus status,
        String name,
        String description,
        RestaurantApplicationAddressResponse address,
        boolean detailsComplete,
        boolean addressComplete,
        boolean hoursComplete,
        boolean imagesComplete,
        boolean documentsComplete,
        List<RestaurantApplicationHoursResponse> hours,
        List<RestaurantApplicationImageResponse> images,
        List<RestaurantApplicationDocumentResponse> documents,
        String rejectionRemarks,
        UUID restaurantId,
        Instant createdAt,
        Instant updatedAt
) {
    public static RestaurantApplicationResponse from(RestaurantApplication app) {
        RestaurantApplicationAddressResponse address = null;
        if (app.getAddressStreet() != null) {
            Double lat = app.getAddressLocation() != null ? app.getAddressLocation().getY() : null;
            Double lon = app.getAddressLocation() != null ? app.getAddressLocation().getX() : null;
            address = new RestaurantApplicationAddressResponse(
                    app.getAddressStreet(), app.getAddressCity(), app.getAddressState(),
                    app.getAddressCountry(), app.getAddressPostalCode(), app.getAddressHouseNumber(),
                    app.getAddressBuildingName(), app.getAddressLandmark(), lat, lon
            );
        }
        return new RestaurantApplicationResponse(
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
                app.getHours().stream().map(RestaurantApplicationHoursResponse::from).toList(),
                app.getImages().stream().map(RestaurantApplicationImageResponse::from).toList(),
                app.getDocuments().stream().map(RestaurantApplicationDocumentResponse::from).toList(),
                app.getRejectionRemarks(),
                app.getRestaurant() != null ? app.getRestaurant().getId() : null,
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }
}
