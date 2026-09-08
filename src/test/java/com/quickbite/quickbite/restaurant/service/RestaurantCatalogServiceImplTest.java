package com.quickbite.quickbite.restaurant.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.restaurant.dto.NearbyRestaurantResponse;
import com.quickbite.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.quickbite.restaurant.dto.RestaurantSummaryResponse;
import com.quickbite.quickbite.restaurant.exception.RestaurantNotFoundException;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.model.RestaurantVerificationStatus;
import com.quickbite.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.user.model.User;
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
class RestaurantCatalogServiceImplTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private RestaurantCatalogServiceImpl restaurantCatalogService;

    private Restaurant restaurant;
    private UUID restaurantId;

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();

        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setName("Mario Rossi");

        Address address = new Address();
        address.setId(UUID.randomUUID());
        address.setStreet("123 Main St");
        address.setCity("Rome");

        restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setName("Trattoria Mario");
        restaurant.setDescription("Authentic Roman pizza");
        restaurant.setOwner(owner);
        restaurant.setAddress(address);
        restaurant.setAvgRating(BigDecimal.valueOf(4.8));
        restaurant.setTotalRating(120L);
        restaurant.setClosed(false);
        restaurant.setCurrentStatus(RestaurantVerificationStatus.APPROVED);
        restaurant.setCreatedAt(Instant.now());
        restaurant.setRestaurantHours(new ArrayList<>());
        restaurant.setRestaurantImages(new ArrayList<>());
    }

    @Test
    @DisplayName("Returns approved restaurant to public")
    void getRestaurant_approved() {
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

        RestaurantResponse res = restaurantCatalogService.getRestaurant(restaurantId);

        assertThat(res.id()).isEqualTo(restaurantId);
        assertThat(res.name()).isEqualTo("Trattoria Mario");
    }

    @Test
    @DisplayName("Throws RestaurantNotFoundException if restaurant is not approved")
    void getRestaurant_notApproved() {
        restaurant.setCurrentStatus(RestaurantVerificationStatus.PENDING);
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> restaurantCatalogService.getRestaurant(restaurantId))
                .isInstanceOf(RestaurantNotFoundException.class);
    }

    @Test
    @DisplayName("Throws RestaurantNotFoundException if restaurant not found in DB")
    void getRestaurant_notFound() {
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantCatalogService.getRestaurant(restaurantId))
                .isInstanceOf(RestaurantNotFoundException.class);
    }

    @Test
    @DisplayName("Returns cursor page of approved restaurants")
    void listApproved_success() {
        when(restaurantRepository.findAllWithCursor(eq(RestaurantVerificationStatus.APPROVED), isNull(), eq(Limit.of(21))))
                .thenReturn(List.of(restaurant));

        CursorPage<RestaurantSummaryResponse> page = restaurantCatalogService.listApproved(null, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).name()).isEqualTo("Trattoria Mario");
    }

    @Test
    @DisplayName("Returns nearby approved restaurants with distance")
    void findNearbyRestaurants_success() {
        when(restaurantRepository.findNearbyRestaurants(12.9716, 77.5946, 5000, 20, 0))
                .thenReturn(List.of(restaurant));

        List<NearbyRestaurantResponse> results =
                restaurantCatalogService.findNearbyRestaurants(12.9716, 77.5946, 5000, 0, 20);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().id()).isEqualTo(restaurantId);
    }
}
