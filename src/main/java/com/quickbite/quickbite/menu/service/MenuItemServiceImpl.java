package com.quickbite.quickbite.menu.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.menu.dto.MenuItemImageRequest;
import com.quickbite.quickbite.menu.dto.MenuItemRequest;
import com.quickbite.quickbite.menu.dto.MenuItemResponse;
import com.quickbite.quickbite.menu.exception.CuisineNotFoundException;
import com.quickbite.quickbite.menu.exception.MenuItemNotFoundException;
import com.quickbite.quickbite.menu.model.Cuisine;
import com.quickbite.quickbite.menu.model.CuisineStatus;
import com.quickbite.quickbite.menu.model.MenuItem;
import com.quickbite.quickbite.menu.model.MenuItemImage;
import com.quickbite.quickbite.menu.repository.CuisineRepository;
import com.quickbite.quickbite.menu.repository.MenuItemImageRepository;
import com.quickbite.quickbite.menu.repository.MenuItemRepository;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.repository.RestaurantRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MenuItemServiceImpl implements MenuItemService {
    private final MenuItemRepository menuItemRepository;
    private final MenuItemImageRepository menuItemImageRepository;
    private final RestaurantRepository restaurantRepository;
    private final CuisineRepository cuisineRepository;

    public MenuItemServiceImpl(
            MenuItemRepository menuItemRepository,
            MenuItemImageRepository menuItemImageRepository,
            RestaurantRepository restaurantRepository,
            CuisineRepository cuisineRepository) {
        this.menuItemRepository = menuItemRepository;
        this.menuItemImageRepository = menuItemImageRepository;
        this.restaurantRepository = restaurantRepository;
        this.cuisineRepository = cuisineRepository;
    }

    /**
     * Creates a new menu item for a restaurant owned by the specified owner.
     *
     * @param restaurantId the ID of the restaurant
     * @param ownerId      the ID of the owner
     * @param req          the request containing menu item details
     * @return the response containing the details of the created menu item
     * @throws ResourceNotFoundException if the restaurant or cuisine is not found
     * @throws AccessDeniedException      if the owner does not own the restaurant
     */
    @Override
    @Transactional
    public MenuItemResponse create(UUID restaurantId, UUID ownerId, MenuItemRequest req) {
        Restaurant restaurant = loadOwnedRestaurant(restaurantId, ownerId);
        Cuisine cuisine = loadApprovedCuisine(req.cuisineId());
        MenuItem menuItem = new MenuItem();
        menuItem.setRestaurant(restaurant);
        return getMenuItemResponse(req, cuisine, menuItem);
    }

    /**
     * Updates an existing menu item for a restaurant owned by the specified owner.
     *
     * @param restaurantId the ID of the restaurant
     * @param itemId         the ID of the menu item to update
     * @param ownerId        the ID of the owner
     * @param req            the request containing updated menu item details
     * @return the response containing the details of the updated menu item
     * @throws ResourceNotFoundException if the restaurant or cuisine is not found
     * @throws AccessDeniedException      if the owner does not own the restaurant
     */
    @Override
    @Transactional
    public MenuItemResponse update(UUID restaurantId, UUID itemId, UUID ownerId, MenuItemRequest req) {
        Restaurant restaurant = loadOwnedRestaurant(restaurantId, ownerId);
        Cuisine cuisine = loadApprovedCuisine(req.cuisineId());
        MenuItem menuItem = menuItemRepository.findByIdAndRestaurantId(itemId, restaurant.getId())
                .orElseThrow(() -> new MenuItemNotFoundException("Item not found"));
        return getMenuItemResponse(req, cuisine, menuItem);
    }

    /**
     * Deletes a menu item for a restaurant owned by the specified owner.
     *
     * @param restaurantId the ID of the restaurant
     * @param itemId       the ID of the menu item to delete
     * @param ownerId      the ID of the owner
     * @throws ResourceNotFoundException if the restaurant or menu item is not found
     * @throws AccessDeniedException      if the owner does not own the restaurant
     */
    @Override
    @Transactional
    public void delete(UUID restaurantId, UUID itemId, UUID ownerId) {
        Restaurant restaurant = loadOwnedRestaurant(restaurantId, ownerId);
        MenuItem menuItem = menuItemRepository.findByIdAndRestaurantId(itemId, restaurant.getId())
                .orElseThrow(() -> new MenuItemNotFoundException("Item not found"));
        menuItemRepository.delete(menuItem);
    }

    /**
     * Adds an image to a menu item for a restaurant owned by the specified owner.
     *
     * @param restaurantId the ID of the restaurant
     * @param itemId       the ID of the menu item
     * @param ownerId      the ID of the owner
     * @param req          the request containing image details
     * @return the response containing the details of the updated menu item
     * @throws ResourceNotFoundException if the restaurant or menu item is not found
     * @throws AccessDeniedException      if the owner does not own the restaurant
     */
    @Override
    @Transactional
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

    /**
     * Removes an image from a menu item for a restaurant owned by the specified owner.
     *
     * @param restaurantId the ID of the restaurant
     * @param itemId       the ID of the menu item
     * @param imageId      the ID of the image to remove
     * @param ownerId      the ID of the owner
     * @throws ResourceNotFoundException if the restaurant, menu item, or image is not found
     * @throws AccessDeniedException      if the owner does not own the restaurant
     */
    @Override
    @Transactional
    public void removeImage(UUID restaurantId, UUID itemId, UUID imageId, UUID ownerId) {
        Restaurant restaurant = loadOwnedRestaurant(restaurantId, ownerId);
        MenuItem menuItem = menuItemRepository.findByIdAndRestaurantId(itemId, restaurant.getId())
                .orElseThrow(() -> new MenuItemNotFoundException("Item not found"));
        MenuItemImage menuItemImage = menuItemImageRepository.findByIdAndMenuItem(imageId, menuItem)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        menuItem.removeImage(menuItemImage);
    }

    /**
     * Lists menu items for a restaurant, optionally filtering by availability.
     *
     * @param restaurantId the ID of the restaurant
     * @param availableOnly whether to filter by available items only
     * @param cursor        the cursor for pagination
     * @param size          the number of items to return
     * @return a paginated response containing the list of menu items
     */
    @Override
    @Transactional(readOnly = true)
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

    /**
     * Retrieves a menu item by its ID for a specific restaurant.
     *
     * @param restaurantId the ID of the restaurant
     * @param itemId       the ID of the menu item
     * @return the response containing the details of the menu item
     * @throws ResourceNotFoundException if the menu item is not found
     */
    @Override
    @Transactional(readOnly = true)
    public MenuItemResponse getById(UUID restaurantId, UUID itemId) {
        return MenuItemResponse.from(
                menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)
                        .orElseThrow(() -> new MenuItemNotFoundException("Item not found"))
        );
    }

    /**
     * Loads a restaurant owned by the specified owner.
     *
     * @param restaurantId the ID of the restaurant
     * @param ownerId      the ID of the owner
     * @return the restaurant entity
     * @throws ResourceNotFoundException if the restaurant is not found
     * @throws AccessDeniedException      if the owner does not own the restaurant
     */
    private Restaurant loadOwnedRestaurant(UUID restaurantId, UUID ownerId) {
        Restaurant r = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        if (!r.getOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException("Access denied, You are not the owner of this restaurant");
        }
        return r;
    }

    /**
     * Loads an approved cuisine by its ID.
     *
     * @param cuisineId the ID of the cuisine
     * @return the cuisine entity
     * @throws CuisineNotFoundException if the cuisine is not found
     * @throws BadRequestException       if the cuisine is not approved
     */
    private Cuisine loadApprovedCuisine(UUID cuisineId) {
        return cuisineRepository.findById(cuisineId)
                .orElseThrow(() -> new CuisineNotFoundException("Cuisine not found"));
    }

    /**
     * Updates the menu item entity with the request data and saves it to the repository.
     *
     * @param req      the request containing menu item details
     * @param cuisine  the cuisine entity associated with the menu item
     * @param menuItem the menu item entity to update
     * @return the response containing the details of the saved menu item
     */
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
