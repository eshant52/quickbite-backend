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
import com.quickbite.quickbite.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuItemManagementServiceImplTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private MenuItemImageRepository menuItemImageRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private CuisineRepository cuisineRepository;

    @InjectMocks
    private MenuItemManagementServiceImpl menuItemManagementService;

    private UUID restaurantId;
    private UUID ownerId;
    private UUID cuisineId;
    private UUID itemId;
    private Restaurant restaurant;
    private Cuisine cuisine;
    private MenuItem menuItem;

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        cuisineId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        User owner = new User();
        owner.setId(ownerId);

        restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setOwner(owner);

        cuisine = new Cuisine();
        cuisine.setId(cuisineId);
        cuisine.setName("Italian");

        menuItem = new MenuItem();
        menuItem.setId(itemId);
        menuItem.setName("Pasta Carbonara");
        menuItem.setDescription("Classic Roman pasta");
        menuItem.setPrice(BigDecimal.valueOf(14.00));
        menuItem.setCategory("Pasta");
        menuItem.setAvailable(true);
        menuItem.setRestaurant(restaurant);
        menuItem.setCuisine(cuisine);
        menuItem.setCreatedAt(Instant.now());
        menuItem.setImages(new ArrayList<>());
    }

    @Test
    @DisplayName("create successfully creates and returns new menu item")
    void create_success() {
        MenuItemRequest req = new MenuItemRequest("Pasta Carbonara", "Classic Roman pasta", cuisineId, BigDecimal.valueOf(14.00), "Pasta", true);

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(cuisineRepository.findById(cuisineId)).thenReturn(Optional.of(cuisine));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(i -> {
            MenuItem m = i.getArgument(0);
            m.setId(itemId);
            m.setCreatedAt(Instant.now());
            return m;
        });

        MenuItemResponse res = menuItemManagementService.create(restaurantId, ownerId, req);

        assertThat(res.name()).isEqualTo("Pasta Carbonara");
        assertThat(res.price()).isEqualTo(BigDecimal.valueOf(14.00));
    }

    @Test
    @DisplayName("create throws AccessDeniedException when caller is not the restaurant owner")
    void create_notOwner() {
        UUID wrongOwnerId = UUID.randomUUID();
        MenuItemRequest req = new MenuItemRequest("Pasta Carbonara", "Classic Roman pasta", cuisineId, BigDecimal.valueOf(14.00), "Pasta", true);

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> menuItemManagementService.create(restaurantId, wrongOwnerId, req))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("create throws CuisineNotFoundException when cuisine does not exist")
    void create_cuisineNotFound() {
        MenuItemRequest req = new MenuItemRequest("Pasta Carbonara", "Classic Roman pasta", cuisineId, BigDecimal.valueOf(14.00), "Pasta", true);

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(cuisineRepository.findById(cuisineId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuItemManagementService.create(restaurantId, ownerId, req))
                .isInstanceOf(CuisineNotFoundException.class);
    }

    @Test
    @DisplayName("update successfully modifies menu item")
    void update_success() {
        MenuItemRequest req = new MenuItemRequest("Pasta Carbonara (Large)", "Even more cheese", cuisineId, BigDecimal.valueOf(18.00), "Pasta", true);

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(cuisineRepository.findById(cuisineId)).thenReturn(Optional.of(cuisine));
        when(menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)).thenReturn(Optional.of(menuItem));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(i -> i.getArgument(0));

        MenuItemResponse res = menuItemManagementService.update(restaurantId, itemId, ownerId, req);

        assertThat(res.name()).isEqualTo("Pasta Carbonara (Large)");
        assertThat(res.price()).isEqualTo(BigDecimal.valueOf(18.00));
    }

    @Test
    @DisplayName("delete removes menu item from repository")
    void delete_success() {
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)).thenReturn(Optional.of(menuItem));

        menuItemManagementService.delete(restaurantId, itemId, ownerId);

        verify(menuItemRepository).delete(menuItem);
    }

    @Test
    @DisplayName("addImage attaches new image to menu item")
    void addImage_success() {
        MenuItemImageRequest req = new MenuItemImageRequest("https://img.com/pasta.jpg", 1);

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)).thenReturn(Optional.of(menuItem));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(i -> i.getArgument(0));

        MenuItemResponse res = menuItemManagementService.addImage(restaurantId, itemId, ownerId, req);

        assertThat(res.images()).hasSize(1);
        assertThat(res.images().get(0).imageUrl()).isEqualTo("https://img.com/pasta.jpg");
    }

    @Test
    @DisplayName("removeImage deletes image from menu item")
    void removeImage_success() {
        UUID imageId = UUID.randomUUID();
        MenuItemImage image = new MenuItemImage();
        image.setId(imageId);
        menuItem.getImages().add(image);

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId)).thenReturn(Optional.of(menuItem));
        when(menuItemImageRepository.findByIdAndMenuItem(imageId, menuItem)).thenReturn(Optional.of(image));

        menuItemManagementService.removeImage(restaurantId, itemId, imageId, ownerId);

        assertThat(menuItem.getImages()).isEmpty();
    }
}
