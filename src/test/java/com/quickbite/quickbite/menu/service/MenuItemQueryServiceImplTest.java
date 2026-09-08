package com.quickbite.quickbite.menu.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.menu.dto.MenuItemResponse;
import com.quickbite.quickbite.menu.exception.MenuItemNotFoundException;
import com.quickbite.quickbite.menu.model.Cuisine;
import com.quickbite.quickbite.menu.model.MenuItem;
import com.quickbite.quickbite.menu.repository.MenuItemRepository;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuItemQueryServiceImplTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @InjectMocks
    private MenuItemQueryServiceImpl menuItemQueryService;

    private UUID restaurantId;
    private UUID itemId;
    private MenuItem menuItem;

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);

        Cuisine cuisine = new Cuisine();
        cuisine.setId(UUID.randomUUID());
        cuisine.setName("Italian");

        menuItem = new MenuItem();
        menuItem.setId(itemId);
        menuItem.setName("Margherita Pizza");
        menuItem.setDescription("Classic cheese and tomato");
        menuItem.setPrice(BigDecimal.valueOf(12.50));
        menuItem.setCategory("Pizza");
        menuItem.setAvailable(true);
        menuItem.setRestaurant(restaurant);
        menuItem.setCuisine(cuisine);
        menuItem.setCreatedAt(Instant.now());
        menuItem.setImages(new ArrayList<>());
    }

    @Test
    @DisplayName("listByRestaurant returns paginated items")
    void listByRestaurant_success() {
        when(menuItemRepository.findByRestaurantWithCursor(eq(restaurantId), eq(true), isNull(), eq(Limit.of(21))))
                .thenReturn(List.of(menuItem));

        CursorPage<MenuItemResponse> page = menuItemQueryService.listByRestaurant(restaurantId, true, null, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).name()).isEqualTo("Margherita Pizza");
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    @DisplayName("getById returns item when found")
    void getById_success() {
        when(menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId))
                .thenReturn(Optional.of(menuItem));

        MenuItemResponse res = menuItemQueryService.getById(restaurantId, itemId);

        assertThat(res.id()).isEqualTo(itemId);
        assertThat(res.name()).isEqualTo("Margherita Pizza");
    }

    @Test
    @DisplayName("getById throws MenuItemNotFoundException when not found")
    void getById_notFound() {
        when(menuItemRepository.findByIdAndRestaurantId(itemId, restaurantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuItemQueryService.getById(restaurantId, itemId))
                .isInstanceOf(MenuItemNotFoundException.class);
    }
}
