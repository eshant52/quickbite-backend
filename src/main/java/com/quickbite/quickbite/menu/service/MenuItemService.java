package com.quickbite.quickbite.menu.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.menu.dto.MenuItemImageRequest;
import com.quickbite.quickbite.menu.dto.MenuItemRequest;
import com.quickbite.quickbite.menu.dto.MenuItemResponse;

import java.util.UUID;

public interface MenuItemService {
    // Owner writes
    MenuItemResponse create(UUID restaurantId, UUID ownerId, MenuItemRequest req);
    MenuItemResponse update(UUID restaurantId, UUID itemId, UUID ownerId, MenuItemRequest req);
    void delete(UUID restaurantId, UUID itemId, UUID ownerId);
    MenuItemResponse addImage(UUID restaurantId, UUID itemId, UUID ownerId, MenuItemImageRequest req);
    void removeImage(UUID restaurantId, UUID itemId, UUID imageId, UUID ownerId);

    // Public reads
    CursorPage<MenuItemResponse> listByRestaurant(UUID restaurantId, boolean availableOnly, UUID cursor, int size);
    MenuItemResponse getById(UUID restaurantId, UUID itemId);
}
