package com.quickbite.quickbite.menu.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.menu.dto.MenuItemResponse;

import java.util.UUID;

public interface MenuItemQueryService {
    CursorPage<MenuItemResponse> listByRestaurant(UUID restaurantId, boolean availableOnly, UUID cursor, int size);
    MenuItemResponse getById(UUID restaurantId, UUID itemId);
}
