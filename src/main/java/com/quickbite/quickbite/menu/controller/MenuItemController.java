package com.quickbite.quickbite.menu.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.menu.dto.MenuItemImageRequest;
import com.quickbite.quickbite.menu.dto.MenuItemRequest;
import com.quickbite.quickbite.menu.dto.MenuItemResponse;
import com.quickbite.quickbite.menu.service.MenuItemService;
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
public class MenuItemController {
    private final MenuItemService menuItemService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public MenuItemController(
            MenuItemService menuItemService,
            AuthenticatedSessionResolver authenticatedSessionResolver) {
        this.menuItemService = menuItemService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    @GetMapping
    public ResponseEntity<CursorPage<MenuItemResponse>> getMenuItems(
            @PathVariable UUID restaurantId,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "true") boolean available
    ) {
        return ResponseEntity.ok(menuItemService.listByRestaurant(restaurantId, available, cursor, size));
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<MenuItemResponse> getMenuItem(
            @PathVariable UUID restaurantId,
            @PathVariable UUID itemId
    ) {
        return ResponseEntity.ok(menuItemService.getById(restaurantId, itemId));
    }

    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<MenuItemResponse> createMenuItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @RequestBody @Valid MenuItemRequest menuItemRequest
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(menuItemService.create(restaurantId, ownerId, menuItemRequest));
    }

    @PutMapping("/{itemId}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<MenuItemResponse> updateMenuItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @PathVariable UUID itemId,
            @RequestBody @Valid MenuItemRequest menuItemRequest
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(menuItemService.update(restaurantId, itemId, ownerId, menuItemRequest));
    }

    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<Void> deleteMenuItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @PathVariable UUID itemId
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        menuItemService.delete(restaurantId, itemId, ownerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{itemId}/images")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<MenuItemResponse> createMenuItemImage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @PathVariable UUID itemId,
            @RequestBody @Valid MenuItemImageRequest req
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(menuItemService.addImage(restaurantId, itemId, ownerId, req));
    }

    @DeleteMapping("/{itemId}/images/{imageId}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<Void> deleteMenuItemImage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @PathVariable UUID itemId,
            @PathVariable UUID imageId
    ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        menuItemService.removeImage(restaurantId, itemId, imageId, ownerId);
        return ResponseEntity.noContent().build();
    }
}
