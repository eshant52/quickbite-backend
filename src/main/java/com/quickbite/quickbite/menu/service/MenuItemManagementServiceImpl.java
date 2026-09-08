package com.quickbite.quickbite.menu.service;

import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.menu.dto.MenuItemImageRequest;
import com.quickbite.quickbite.menu.dto.MenuItemRequest;
import com.quickbite.quickbite.menu.dto.MenuItemResponse;
import com.quickbite.quickbite.menu.exception.CuisineNotFoundException;
import com.quickbite.quickbite.menu.exception.MenuItemNotFoundException;
import com.quickbite.quickbite.menu.model.Cuisine;
import com.quickbite.quickbite.menu.model.MenuItem;
import com.quickbite.quickbite.menu.model.MenuItemImage;
import com.quickbite.quickbite.menu.repository.CuisineRepository;
import com.quickbite.quickbite.menu.repository.MenuItemImageRepository;
import com.quickbite.quickbite.menu.repository.MenuItemRepository;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.repository.RestaurantRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class MenuItemManagementServiceImpl implements MenuItemManagementService {

    private final MenuItemRepository menuItemRepository;
    private final MenuItemImageRepository menuItemImageRepository;
    private final RestaurantRepository restaurantRepository;
    private final CuisineRepository cuisineRepository;

    public MenuItemManagementServiceImpl(
            MenuItemRepository menuItemRepository,
            MenuItemImageRepository menuItemImageRepository,
            RestaurantRepository restaurantRepository,
            CuisineRepository cuisineRepository
    ) {
        this.menuItemRepository = menuItemRepository;
        this.menuItemImageRepository = menuItemImageRepository;
        this.restaurantRepository = restaurantRepository;
        this.cuisineRepository = cuisineRepository;
    }

    @Override
    public MenuItemResponse create(UUID restaurantId, UUID ownerId, MenuItemRequest req) {
        Restaurant restaurant = loadOwnedRestaurant(restaurantId, ownerId);
        Cuisine cuisine = loadApprovedCuisine(req.cuisineId());
        MenuItem menuItem = new MenuItem();
        menuItem.setRestaurant(restaurant);
        return getMenuItemResponse(req, cuisine, menuItem);
    }

    @Override
    public MenuItemResponse update(UUID restaurantId, UUID itemId, UUID ownerId, MenuItemRequest req) {
        Restaurant restaurant = loadOwnedRestaurant(restaurantId, ownerId);
        Cuisine cuisine = loadApprovedCuisine(req.cuisineId());
        MenuItem menuItem = menuItemRepository.findByIdAndRestaurantId(itemId, restaurant.getId())
                .orElseThrow(() -> new MenuItemNotFoundException("Item not found"));
        return getMenuItemResponse(req, cuisine, menuItem);
    }

    @Override
    public void delete(UUID restaurantId, UUID itemId, UUID ownerId) {
        Restaurant restaurant = loadOwnedRestaurant(restaurantId, ownerId);
        MenuItem menuItem = menuItemRepository.findByIdAndRestaurantId(itemId, restaurant.getId())
                .orElseThrow(() -> new MenuItemNotFoundException("Item not found"));
        menuItemRepository.delete(menuItem);
    }

    @Override
    public MenuItemResponse addImage(UUID restaurantId, UUID itemId, UUID ownerId, MenuItemImageRequest req) {
        Restaurant restaurant = loadOwnedRestaurant(restaurantId, ownerId);
        MenuItem menuItem = menuItemRepository.findByIdAndRestaurantId(itemId, restaurant.getId())
                .orElseThrow(() -> new MenuItemNotFoundException("Item not found"));

        MenuItemImage menuItemImage = new MenuItemImage();
        menuItemImage.setImageUrl(req.imageUrl());
        menuItemImage.setDisplayOrder(req.displayOrder());

        menuItem.addImage(menuItemImage);
        MenuItem menuItemUpdated = menuItemRepository.save(menuItem);

        return MenuItemResponse.from(menuItemUpdated);
    }

    @Override
    public void removeImage(UUID restaurantId, UUID itemId, UUID imageId, UUID ownerId) {
        Restaurant restaurant = loadOwnedRestaurant(restaurantId, ownerId);
        MenuItem menuItem = menuItemRepository.findByIdAndRestaurantId(itemId, restaurant.getId())
                .orElseThrow(() -> new MenuItemNotFoundException("Item not found"));
        MenuItemImage menuItemImage = menuItemImageRepository.findByIdAndMenuItem(imageId, menuItem)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        menuItem.removeImage(menuItemImage);
    }

    private Restaurant loadOwnedRestaurant(UUID restaurantId, UUID ownerId) {
        Restaurant r = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        if (!r.getOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException("Access denied, You are not the owner of this restaurant");
        }
        return r;
    }

    private Cuisine loadApprovedCuisine(UUID cuisineId) {
        return cuisineRepository.findById(cuisineId)
                .orElseThrow(() -> new CuisineNotFoundException("Cuisine not found"));
    }

    @NonNull
    private MenuItemResponse getMenuItemResponse(MenuItemRequest req, Cuisine cuisine, MenuItem menuItem) {
        menuItem.setName(req.name());
        menuItem.setDescription(req.description());
        menuItem.setCuisine(cuisine);
        menuItem.setPrice(req.price());
        menuItem.setCategory(req.category());
        menuItem.setAvailable(req.isAvailable());
        MenuItem savedMenuItem = menuItemRepository.save(menuItem);
        return MenuItemResponse.from(savedMenuItem);
    }
}
