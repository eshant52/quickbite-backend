package com.quickbite.quickbite.onboarding.service.restaurant;

import com.quickbite.quickbite.onboarding.dto.restaurant.*;
import com.quickbite.quickbite.restaurant.model.RestaurantDocumentType;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing restaurant applications.
 */
public interface RestaurantApplicationService {
    RestaurantApplicationResponse startApplication(UUID ownerId);

    RestaurantApplicationResponse getCurrentApplication(UUID ownerId);

    RestaurantApplicationResponse getApplication(UUID appId, UUID ownerId);

    RestaurantApplicationDetailsResponse getDetails(UUID appId, UUID ownerId);

    RestaurantApplicationDetailsResponse saveDetails(UUID appId, UUID ownerId, RestaurantApplicationDetailsRequest request);

    RestaurantApplicationAddressResponse getAddress(UUID appId, UUID ownerId);

    RestaurantApplicationAddressResponse saveAddress(UUID appId, UUID ownerId, RestaurantApplicationAddressRequest request);

    List<RestaurantApplicationHoursResponse> getHours(UUID appId, UUID ownerId);

    List<RestaurantApplicationHoursResponse> saveHours(UUID appId, UUID ownerId, RestaurantApplicationHoursRequest request);

    List<RestaurantApplicationImageResponse> getImage(UUID appId, UUID ownerId);

    RestaurantApplicationImageResponse addImage(UUID appId, UUID ownerId, RestaurantApplicationImageRequest request);

    void removeImage(UUID appId, UUID ownerId, UUID imageId);

    List<RestaurantApplicationDocumentResponse> getDocuments(UUID appId, UUID ownerId);

    RestaurantApplicationDocumentResponse addDocument(UUID appId, UUID ownerId, RestaurantApplicationDocumentRequest request);

    void removeDocument(UUID appId, UUID ownerId, RestaurantDocumentType type);

    RestaurantApplicationResponse submitApplication(UUID appId, UUID ownerId);

    RestaurantApplicationResponse reopenApplication(UUID appId, UUID ownerId);
}
