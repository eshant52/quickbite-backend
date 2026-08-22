package com.quickbite.quickbite.menu.dto;

import com.quickbite.quickbite.menu.model.MenuItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MenuItemResponse(
        UUID id,
        UUID restaurantId,
        String name,
        String description,
        CuisineResponse cuisine,
        BigDecimal price,
        String category,
        boolean isAvailable,
        List<MenuItemImageResponse> images,
        Instant createdAt,
        Instant updatedAt) {
    public static MenuItemResponse from(MenuItem menuItem) {
        return new MenuItemResponse(
                menuItem.getId(),
                menuItem.getRestaurant().getId(),
                menuItem.getName(),
                menuItem.getDescription(),
                CuisineResponse.from(menuItem.getCuisine()),
                menuItem.getPrice(),
                menuItem.getCategory(),
                menuItem.isAvailable(),
                menuItem.getImages().stream()
                        .map(MenuItemImageResponse::from)
                        .toList(),
                menuItem.getCreatedAt(),
                menuItem.getUpdatedAt()
        );
    }
}
