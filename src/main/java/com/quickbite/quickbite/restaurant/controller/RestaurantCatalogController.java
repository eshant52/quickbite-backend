package com.quickbite.quickbite.restaurant.controller;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.restaurant.dto.NearbyRestaurantResponse;
import com.quickbite.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.quickbite.restaurant.dto.RestaurantSummaryResponse;
import com.quickbite.quickbite.restaurant.service.RestaurantCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants")
public class RestaurantCatalogController {

    private final RestaurantCatalogService restaurantCatalogService;

    public RestaurantCatalogController(RestaurantCatalogService restaurantCatalogService) {
        this.restaurantCatalogService = restaurantCatalogService;
    }

    @GetMapping
    public ResponseEntity<CursorPage<RestaurantSummaryResponse>> listApproved(
            @RequestParam(value = "cursor", required = false) UUID cursor,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(restaurantCatalogService.listApproved(cursor, size));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyRestaurantResponse>> findNearbyRestaurants(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5000") int radius,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(restaurantCatalogService.findNearbyRestaurants(lat, lng, radius, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getRestaurant(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(restaurantCatalogService.getRestaurant(id));
    }
}
