package com.quickbite.quickbite.menu.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.menu.dto.MenuItemImageRequest;
import com.quickbite.quickbite.menu.dto.MenuItemRequest;
import com.quickbite.quickbite.menu.dto.MenuItemResponse;
import com.quickbite.quickbite.menu.service.MenuItemManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/menu-items")
@PreAuthorize("hasRole('RESTAURANT_OWNER')")
public class MenuItemOwnerController {

    private final MenuItemManagementService menuItemManagementService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public MenuItemOwnerController(
            MenuItemManagementService menuItemManagementService,
            AuthenticatedSessionResolver authenticatedSessionResolver
    ) {
        this.menuItemManagementService = menuItemManagementService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    @PostMapping
    public ResponseEntity<MenuItemResponse> createMenuItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @RequestBody @Valid MenuItemRequest menuItemRequest
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(menuItemManagementService.create(restaurantId, ownerId, menuItemRequest));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<MenuItemResponse> updateMenuItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @PathVariable UUID itemId,
            @RequestBody @Valid MenuItemRequest menuItemRequest
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(menuItemManagementService.update(restaurantId, itemId, ownerId, menuItemRequest));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteMenuItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @PathVariable UUID itemId
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        menuItemManagementService.delete(restaurantId, itemId, ownerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{itemId}/images")
    public ResponseEntity<MenuItemResponse> createMenuItemImage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @PathVariable UUID itemId,
            @RequestBody @Valid MenuItemImageRequest req
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(menuItemManagementService.addImage(restaurantId, itemId, ownerId, req));
    }

    @DeleteMapping("/{itemId}/images/{imageId}")
    public ResponseEntity<Void> deleteMenuItemImage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @PathVariable UUID itemId,
            @PathVariable UUID imageId
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        menuItemManagementService.removeImage(restaurantId, itemId, imageId, ownerId);
        return ResponseEntity.noContent().build();
    }
}
