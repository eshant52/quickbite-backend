package com.quickbite.quickbite.restaurant.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.restaurant.dto.*;
import com.quickbite.quickbite.restaurant.model.RestaurantVerificationStatus;

import java.util.List;
import java.util.UUID;

public interface RestaurantService {
    CursorPage<RestaurantSummaryResponse> listMyRestaurants(UUID ownerId, RestaurantVerificationStatus status,  UUID cursor, int size);
    RestaurantResponse getMyRestaurant(UUID restaurantId, UUID ownerId);
    RestaurantResponse update(UUID restaurantId, UUID ownerId, UpdateRestaurantRequest req);
    RestaurantResponse setHours(UUID restaurantId, UUID ownerId, List<RestaurantHoursRequest> hours);
    RestaurantResponse addImage(UUID restaurantId, UUID ownerId, String imageUrl, int displayOrder);
    RestaurantResponse removeImage(UUID restaurantId, UUID ownerId, UUID imageId);
    RestaurantResponse toggleClosed(UUID restaurantId, UUID ownerId);

    // public restaurant catalog
    RestaurantResponse getRestaurant(UUID restaurantId);
    CursorPage<RestaurantSummaryResponse> listApproved(UUID cursor, int size);

    /**
     * Find approved, open restaurants within {@code radiusMeters} of the given coordinate,
     * ordered by straight-line distance ascending.
     * Uses PostGIS {@code ST_DWithin} + offset-based pagination.
     */
    List<NearbyRestaurantResponse> findNearbyRestaurants(double lat, double lng, int radiusMeters, int page, int size);
}

