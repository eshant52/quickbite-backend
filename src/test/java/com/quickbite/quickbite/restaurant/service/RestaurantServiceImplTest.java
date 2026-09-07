package com.quickbite.quickbite.restaurant.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.routing.adapter.HaversineFallbackAdapter;
import com.quickbite.quickbite.restaurant.dto.*;
import com.quickbite.quickbite.restaurant.exception.RestaurantNotFoundException;
import com.quickbite.quickbite.restaurant.model.*;
import com.quickbite.quickbite.restaurant.repository.RestaurantHoursRepository;
import com.quickbite.quickbite.restaurant.repository.RestaurantImageRepository;
import com.quickbite.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private RestaurantHoursRepository restaurantHoursRepository;

    @Mock
    private RestaurantImageRepository restaurantImageRepository;

    @org.mockito.Spy
    private HaversineFallbackAdapter haversine = new HaversineFallbackAdapter();

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    private User owner;
    private Restaurant restaurant;
    private UUID ownerId;
    private UUID restaurantId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();

        owner = new User();
        owner.setId(ownerId);
        owner.setName("Mario Rossi");
        owner.setEmail("mario@pizza.com");

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

    @Nested
    @DisplayName("listMyRestaurants")
    class ListMyRestaurantsTests {

        @Test
        @DisplayName("Returns cursor-paginated list of owned restaurants")
        void listMyRestaurants_success() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(restaurantRepository.findByOwnerWithCursor(eq(owner), eq(RestaurantVerificationStatus.APPROVED), any(), any()))
                    .thenReturn(List.of(restaurant));

            CursorPage<RestaurantSummaryResponse> page = restaurantService.listMyRestaurants(
                    ownerId, RestaurantVerificationStatus.APPROVED, null, 20);

            assertThat(page.content()).hasSize(1);
            assertThat(page.content().get(0).name()).isEqualTo("Trattoria Mario");
        }
    }

    @Nested
    @DisplayName("getMyRestaurant")
    class GetMyRestaurantTests {

        @Test
        @DisplayName("Returns full details of owned restaurant")
        void getMyRestaurant_success() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(restaurantRepository.findByIdAndOwner(restaurantId, owner)).thenReturn(Optional.of(restaurant));

            RestaurantResponse res = restaurantService.getMyRestaurant(restaurantId, ownerId);

            assertThat(res.id()).isEqualTo(restaurantId);
            assertThat(res.name()).isEqualTo("Trattoria Mario");
            assertThat(res.ownerId()).isEqualTo(ownerId);
        }

        @Test
        @DisplayName("Throws RestaurantNotFoundException when restaurant does not belong to owner")
        void getMyRestaurant_notFound() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(restaurantRepository.findByIdAndOwner(restaurantId, owner)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> restaurantService.getMyRestaurant(restaurantId, ownerId))
                    .isInstanceOf(RestaurantNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("Updates restaurant name and description")
        void update_success() {
            UpdateRestaurantRequest req = new UpdateRestaurantRequest("New Name", "New Description");
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(restaurantRepository.findByIdAndOwner(restaurantId, owner)).thenReturn(Optional.of(restaurant));
            when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(i -> i.getArgument(0));

            RestaurantResponse res = restaurantService.update(restaurantId, ownerId, req);

            assertThat(res.name()).isEqualTo("New Name");
            assertThat(res.description()).isEqualTo("New Description");
        }
    }

    @Nested
    @DisplayName("setHours")
    class SetHoursTests {

        @Test
        @DisplayName("Replaces operating hours and syncs in-memory collection")
        void setHours_success() {
            List<RestaurantHoursRequest> hoursReq = List.of(
                    new RestaurantHoursRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(22, 0))
            );

            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(restaurantRepository.findByIdAndOwner(restaurantId, owner)).thenReturn(Optional.of(restaurant));
            when(restaurantHoursRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

            RestaurantResponse res = restaurantService.setHours(restaurantId, ownerId, hoursReq);

            verify(restaurantHoursRepository).deleteAllByRestaurant(restaurant);
            assertThat(res.hours()).hasSize(1);
            assertThat(res.hours().get(0).dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }
    }

    @Nested
    @DisplayName("addImage & removeImage")
    class ImageTests {

        @Test
        @DisplayName("Adds image when below limit")
        void addImage_success() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(restaurantRepository.findByIdAndOwner(restaurantId, owner)).thenReturn(Optional.of(restaurant));
            when(restaurantImageRepository.countByRestaurant(restaurant)).thenReturn(2L);

            RestaurantImage savedImg = new RestaurantImage();
            savedImg.setId(UUID.randomUUID());
            savedImg.setImageUrl("https://img.com/pizza.jpg");
            savedImg.setDisplayOrder(1);
            when(restaurantImageRepository.save(any(RestaurantImage.class))).thenReturn(savedImg);

            RestaurantResponse res = restaurantService.addImage(restaurantId, ownerId, "https://img.com/pizza.jpg", 1);

            assertThat(res.images()).hasSize(1);
            assertThat(res.images().get(0).imageUrl()).isEqualTo("https://img.com/pizza.jpg");
        }

        @Test
        @DisplayName("Throws BadRequestException when max images exceeded")
        void addImage_maxImages() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(restaurantRepository.findByIdAndOwner(restaurantId, owner)).thenReturn(Optional.of(restaurant));
            when(restaurantImageRepository.countByRestaurant(restaurant)).thenReturn(10L);

            assertThatThrownBy(() -> restaurantService.addImage(restaurantId, ownerId, "https://img.com/pizza.jpg", 1))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("more than 10 images");
        }

        @Test
        @DisplayName("Removes image and updates in-memory collection")
        void removeImage_success() {
            UUID imageId = UUID.randomUUID();
            RestaurantImage img = new RestaurantImage();
            img.setId(imageId);
            restaurant.getRestaurantImages().add(img);

            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(restaurantRepository.findByIdAndOwner(restaurantId, owner)).thenReturn(Optional.of(restaurant));
            when(restaurantImageRepository.findByIdAndRestaurant(imageId, restaurant)).thenReturn(Optional.of(img));

            RestaurantResponse res = restaurantService.removeImage(restaurantId, ownerId, imageId);

            verify(restaurantImageRepository).delete(img);
            assertThat(res.images()).isEmpty();
        }
    }

    @Nested
    @DisplayName("toggleClosed")
    class ToggleClosedTests {

        @Test
        @DisplayName("Toggles closed status between true and false")
        void toggleClosed_success() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(restaurantRepository.findByIdAndOwner(restaurantId, owner)).thenReturn(Optional.of(restaurant));
            when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(i -> i.getArgument(0));

            RestaurantResponse res = restaurantService.toggleClosed(restaurantId, ownerId);

            assertThat(res.isClosed()).isTrue();
        }
    }

    @Nested
    @DisplayName("public getRestaurant & listApproved")
    class PublicCatalogTests {

        @Test
        @DisplayName("Returns approved restaurant to public")
        void getRestaurant_approved() {
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

            RestaurantResponse res = restaurantService.getRestaurant(restaurantId);

            assertThat(res.id()).isEqualTo(restaurantId);
        }

        @Test
        @DisplayName("Throws RestaurantNotFoundException if restaurant is not approved")
        void getRestaurant_notApproved() {
            restaurant.setCurrentStatus(RestaurantVerificationStatus.PENDING);
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

            assertThatThrownBy(() -> restaurantService.getRestaurant(restaurantId))
                    .isInstanceOf(RestaurantNotFoundException.class);
        }

        @Test
        @DisplayName("Returns nearby approved restaurants with distance")
        void findNearbyRestaurants_success() {
            when(restaurantRepository.findNearbyRestaurants(12.9716, 77.5946, 5000, 20, 0))
                    .thenReturn(List.of(restaurant));

            List<com.quickbite.quickbite.restaurant.dto.NearbyRestaurantResponse> results =
                    restaurantService.findNearbyRestaurants(12.9716, 77.5946, 5000, 0, 20);

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().id()).isEqualTo(restaurantId);
        }
    }
}
