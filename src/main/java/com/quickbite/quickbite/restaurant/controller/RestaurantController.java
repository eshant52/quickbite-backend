package com.quickbite.quickbite.restaurant.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.restaurant.dto.*;
import com.quickbite.quickbite.restaurant.model.RestaurantVerificationStatus;
import com.quickbite.quickbite.restaurant.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public RestaurantController(
            RestaurantService restaurantService,
            AuthenticatedSessionResolver authenticatedSessionResolver
    ) {
        this.restaurantService = restaurantService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public Catalog Endpoints
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<CursorPage<RestaurantSummaryResponse>> listApproved(
            @RequestParam(value = "cursor", required = false) UUID cursor,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(restaurantService.listApproved(cursor, size));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyRestaurantResponse>> findNearbyRestaurants(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5000") int radius,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(restaurantService.findNearbyRestaurants(lat, lng, radius, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getRestaurant(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(restaurantService.getRestaurant(id));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Owner Management Endpoints (ROLE_RESTAURANT_OWNER)
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/my")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<CursorPage<RestaurantSummaryResponse>> listMyRestaurants(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "status", required = false) RestaurantVerificationStatus status,
            @RequestParam(value = "cursor", required = false) UUID cursor,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantService.listMyRestaurants(ownerId, status, cursor, size));
    }

    @GetMapping("/my/{id}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<RestaurantResponse> getMyRestaurant(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantService.getMyRestaurant(id, ownerId));
    }

    @PutMapping("/my/{id}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<RestaurantResponse> updateRestaurant(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody @Valid UpdateRestaurantRequest request
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantService.update(id, ownerId, request));
    }

    @PutMapping("/my/{id}/hours")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<RestaurantResponse> setRestaurantHours(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody @Valid List<RestaurantHoursRequest> hours
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantService.setHours(id, ownerId, hours));
    }

    @PostMapping("/my/{id}/images")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<RestaurantResponse> addRestaurantImage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody @Valid RestaurantImageRequest request
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantService.addImage(id, ownerId, request.imageUrl(), request.displayOrder()));
    }

    @DeleteMapping("/my/{id}/images/{imageId}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<RestaurantResponse> removeRestaurantImage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID imageId
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantService.removeImage(id, ownerId, imageId));
    }

    @PostMapping("/my/{id}/toggle-closed")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<RestaurantResponse> toggleClosed(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantService.toggleClosed(id, ownerId));
    }
}
