package com.quickbite.quickbite.restaurant.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.restaurant.dto.RestaurantHoursRequest;
import com.quickbite.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.quickbite.restaurant.dto.RestaurantSummaryResponse;
import com.quickbite.quickbite.restaurant.dto.UpdateRestaurantRequest;
import com.quickbite.quickbite.restaurant.model.RestaurantVerificationStatus;

import java.util.List;
import java.util.UUID;

public interface RestaurantOwnerService {
    CursorPage<RestaurantSummaryResponse> listMyRestaurants(UUID ownerId, RestaurantVerificationStatus status, UUID cursor, int size);
    RestaurantResponse getMyRestaurant(UUID restaurantId, UUID ownerId);
    RestaurantResponse update(UUID restaurantId, UUID ownerId, UpdateRestaurantRequest req);
    RestaurantResponse setHours(UUID restaurantId, UUID ownerId, List<RestaurantHoursRequest> hours);
    RestaurantResponse addImage(UUID restaurantId, UUID ownerId, String imageUrl, int displayOrder);
    RestaurantResponse removeImage(UUID restaurantId, UUID ownerId, UUID imageId);
    RestaurantResponse toggleClosed(UUID restaurantId, UUID ownerId);
}
