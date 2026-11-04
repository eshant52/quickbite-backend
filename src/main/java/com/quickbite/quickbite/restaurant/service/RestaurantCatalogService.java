package com.quickbite.quickbite.restaurant.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.restaurant.dto.NearbyRestaurantResponse;
import com.quickbite.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.quickbite.restaurant.dto.RestaurantSummaryResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface RestaurantCatalogService {
    RestaurantResponse getRestaurant(UUID restaurantId);

    CursorPage<RestaurantSummaryResponse> listApproved(String name, UUID cuisineId, BigDecimal minRating, UUID cursor, int size);

    default CursorPage<RestaurantSummaryResponse> listApproved(UUID cursor, int size) {
        return listApproved(null, null, null, cursor, size);
    }

    List<NearbyRestaurantResponse> findNearbyRestaurants(double lat, double lng, int radiusMeters, String name, UUID cuisineId, BigDecimal minRating, int page, int size);

    default List<NearbyRestaurantResponse> findNearbyRestaurants(double lat, double lng, int radiusMeters, int page, int size) {
        return findNearbyRestaurants(lat, lng, radiusMeters, null, null, null, page, size);
    }
}
