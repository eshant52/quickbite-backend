package com.quickbite.quickbite.restaurant.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.restaurant.dto.NearbyRestaurantResponse;
import com.quickbite.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.quickbite.restaurant.dto.RestaurantSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface RestaurantCatalogService {
    RestaurantResponse getRestaurant(UUID restaurantId);
    CursorPage<RestaurantSummaryResponse> listApproved(UUID cursor, int size);
    List<NearbyRestaurantResponse> findNearbyRestaurants(double lat, double lng, int radiusMeters, int page, int size);
}
