package com.quickbite.quickbite.menu.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.menu.dto.MenuItemResponse;
import com.quickbite.quickbite.menu.exception.MenuItemNotFoundException;
import com.quickbite.quickbite.menu.model.MenuItem;
import com.quickbite.quickbite.menu.repository.MenuItemRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MenuItemQueryServiceImpl implements MenuItemQueryService {

    private final MenuItemRepository menuItemRepository;

    public MenuItemQueryServiceImpl(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    @Override
    public CursorPage<MenuItemResponse> listByRestaurant(UUID restaurantId, boolean availableOnly, UUID cursor, int size) {
        int pageSize = Math.clamp(size, 1, 100);
        List<MenuItem> menuItems = menuItemRepository.findByRestaurantWithCursor(
                restaurantId, availableOnly, cursor, Limit.of(pageSize + 1));
        return CursorPage.of(
                menuItems.stream()
                        .map(MenuItemResponse::from)
                        .toList(),
                pageSize,
                MenuItemResponse::id
        );
    }

    @Override
    public MenuItemResponse getById(UUID restaurantId, UUID itemId) {
        return MenuItemResponse.from(
                menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)
                        .orElseThrow(() -> new MenuItemNotFoundException("Item not found"))
        );
    }
}
