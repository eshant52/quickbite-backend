package com.quickbite.quickbite.restaurant.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.common.routing.GeoPoint;
import com.quickbite.quickbite.common.routing.adapter.HaversineFallbackAdapter;
import com.quickbite.quickbite.restaurant.dto.NearbyRestaurantResponse;
import com.quickbite.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.quickbite.restaurant.dto.RestaurantSummaryResponse;
import com.quickbite.quickbite.restaurant.exception.RestaurantNotFoundException;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.model.RestaurantVerificationStatus;
import com.quickbite.quickbite.restaurant.repository.RestaurantRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RestaurantCatalogServiceImpl implements RestaurantCatalogService {

    private final RestaurantRepository restaurantRepository;

    public RestaurantCatalogServiceImpl(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    @Override
    public RestaurantResponse getRestaurant(UUID restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException("Restaurant not found"));

        if (restaurant.getCurrentStatus() != RestaurantVerificationStatus.APPROVED) {
            throw new RestaurantNotFoundException("Restaurant not found");
        }

        return RestaurantResponse.from(restaurant);
    }

    @Override
    public CursorPage<RestaurantSummaryResponse> listApproved(UUID cursor, int size) {
        int pageSize = Math.clamp(size, 1, 100);

        List<Restaurant> restaurants = restaurantRepository
                .findAllWithCursor(
                        RestaurantVerificationStatus.APPROVED,
                        cursor,
                        Limit.of(pageSize + 1)
                );

        return CursorPage.of(
                restaurants.stream()
                        .map(RestaurantSummaryResponse::from)
                        .toList(),
                pageSize,
                RestaurantSummaryResponse::id
        );
    }

    @Override
    public List<NearbyRestaurantResponse> findNearbyRestaurants(double lat, double lng,
                                                                int radiusMeters, int page, int size) {
        int pageSize   = Math.clamp(size, 1, 50);
        int offset     = Math.max(0, page) * pageSize;
        GeoPoint query = GeoPoint.of(lat, lng);

        List<Restaurant> restaurants = restaurantRepository.findNearbyRestaurants(
                lat, lng, radiusMeters, pageSize, offset
        );

        return restaurants.stream()
                .map(r -> {
                    double distMeters = 0.0;
                    if (r.getAddress() != null && r.getAddress().getLocation() != null) {
                        GeoPoint rLoc = GeoPoint.of(
                                r.getAddress().getLocation().getY(),
                                r.getAddress().getLocation().getX()
                        );
                        distMeters = HaversineFallbackAdapter.haversineMeters(query, rLoc);
                    }
                    return NearbyRestaurantResponse.from(r, distMeters);
                })
                .toList();
    }
}
