package com.quickbite.quickbite.menu.dto;

import com.quickbite.quickbite.menu.model.MenuItemImage;

import java.util.UUID;

public record MenuItemImageResponse(
        UUID id,
        String imageUrl,
        int displayOrder) {
    public static MenuItemImageResponse from(MenuItemImage menuItemImage) {
        return new MenuItemImageResponse(
                menuItemImage.getId(),
                menuItemImage.getImageUrl(),
                menuItemImage.getDisplayOrder()
        );
    }
}
