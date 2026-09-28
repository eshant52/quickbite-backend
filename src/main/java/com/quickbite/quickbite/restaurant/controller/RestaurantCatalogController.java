package com.quickbite.quickbite.restaurant.controller;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.restaurant.dto.NearbyRestaurantResponse;
import com.quickbite.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.quickbite.restaurant.dto.RestaurantSummaryResponse;
import com.quickbite.quickbite.restaurant.service.RestaurantCatalogService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
            @RequestParam @DecimalMin(value = "-90.0", message = "Latitude must be >= -90") @DecimalMax(value = "90.0", message = "Latitude must be <= 90") double lat,
            @RequestParam @DecimalMin(value = "-180.0", message = "Longitude must be >= -180") @DecimalMax(value = "180.0", message = "Longitude must be <= 180") double lng,
            @RequestParam(defaultValue = "5000") @Min(value = 100, message = "Radius must be at least 100 meters") @Max(value = 50000, message = "Radius cannot exceed 50000 meters") int radius,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must be >= 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Size must be >= 1") @Max(value = 50, message = "Size cannot exceed 50") int size
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
