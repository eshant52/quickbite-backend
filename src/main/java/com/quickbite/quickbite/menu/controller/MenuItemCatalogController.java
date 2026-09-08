package com.quickbite.quickbite.menu.controller;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.menu.dto.MenuItemResponse;
import com.quickbite.quickbite.menu.service.MenuItemQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/menu-items")
public class MenuItemCatalogController {

    private final MenuItemQueryService menuItemQueryService;

    public MenuItemCatalogController(MenuItemQueryService menuItemQueryService) {
        this.menuItemQueryService = menuItemQueryService;
    }

    @GetMapping
    public ResponseEntity<CursorPage<MenuItemResponse>> getMenuItems(
            @PathVariable UUID restaurantId,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "true") boolean available
    ) {
        return ResponseEntity.ok(menuItemQueryService.listByRestaurant(restaurantId, available, cursor, size));
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<MenuItemResponse> getMenuItem(
            @PathVariable UUID restaurantId,
            @PathVariable UUID itemId
    ) {
        return ResponseEntity.ok(menuItemQueryService.getById(restaurantId, itemId));
    }
}
