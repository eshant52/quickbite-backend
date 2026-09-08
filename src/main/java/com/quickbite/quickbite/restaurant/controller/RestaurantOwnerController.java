package com.quickbite.quickbite.restaurant.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.restaurant.dto.RestaurantHoursRequest;
import com.quickbite.quickbite.restaurant.dto.RestaurantImageRequest;
import com.quickbite.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.quickbite.restaurant.dto.RestaurantSummaryResponse;
import com.quickbite.quickbite.restaurant.dto.UpdateRestaurantRequest;
import com.quickbite.quickbite.restaurant.model.RestaurantVerificationStatus;
import com.quickbite.quickbite.restaurant.service.RestaurantOwnerService;
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
@PreAuthorize("hasRole('RESTAURANT_OWNER')")
public class RestaurantOwnerController {

    private final RestaurantOwnerService restaurantOwnerService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public RestaurantOwnerController(
            RestaurantOwnerService restaurantOwnerService,
            AuthenticatedSessionResolver authenticatedSessionResolver
    ) {
        this.restaurantOwnerService = restaurantOwnerService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    @GetMapping("/my")
    public ResponseEntity<CursorPage<RestaurantSummaryResponse>> listMyRestaurants(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "status", required = false) RestaurantVerificationStatus status,
            @RequestParam(value = "cursor", required = false) UUID cursor,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantOwnerService.listMyRestaurants(ownerId, status, cursor, size));
    }

    @GetMapping("/{id}/owner")
    public ResponseEntity<RestaurantResponse> getMyRestaurant(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantOwnerService.getMyRestaurant(id, ownerId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestaurantResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody @Valid UpdateRestaurantRequest req
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantOwnerService.update(id, ownerId, req));
    }

    @PutMapping("/{id}/hours")
    public ResponseEntity<RestaurantResponse> setHours(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody @Valid List<RestaurantHoursRequest> hours
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantOwnerService.setHours(id, ownerId, hours));
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<RestaurantResponse> addImage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody @Valid RestaurantImageRequest req
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantOwnerService.addImage(id, ownerId, req.imageUrl(), req.displayOrder()));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<RestaurantResponse> removeImage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID imageId
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantOwnerService.removeImage(id, ownerId, imageId));
    }

    @PatchMapping("/{id}/toggle-closed")
    public ResponseEntity<RestaurantResponse> toggleClosed(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantOwnerService.toggleClosed(id, ownerId));
    }
}
