package com.quickbite.quickbite.restaurant.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.restaurant.dto.*;
import com.quickbite.quickbite.restaurant.model.RestaurantVerificationStatus;
import com.quickbite.quickbite.restaurant.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantControllerTest {

    @Mock
    private RestaurantService restaurantService;

    @Mock
    private AuthenticatedSessionResolver authenticatedSessionResolver;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private RestaurantController restaurantController;

    private UUID ownerId;
    private UUID restaurantId;
    private RestaurantResponse mockResponse;
    private RestaurantSummaryResponse mockSummary;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();

        mockSummary = new RestaurantSummaryResponse(
                restaurantId,
                "Trattoria Mario",
                BigDecimal.valueOf(4.8),
                100L,
                false,
                RestaurantVerificationStatus.APPROVED,
                Instant.now()
        );

        mockResponse = new RestaurantResponse(
                restaurantId,
                "Trattoria Mario",
                "Authentic Roman pizza",
                BigDecimal.valueOf(4.8),
                100L,
                false,
                RestaurantVerificationStatus.APPROVED,
                ownerId,
                null,
                List.of(),
                List.of(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("listApproved returns public cursor page")
    void listApproved_success() {
        CursorPage<RestaurantSummaryResponse> page = CursorPage.of(List.of(mockSummary), 20, RestaurantSummaryResponse::id);
        when(restaurantService.listApproved(null, 20)).thenReturn(page);

        ResponseEntity<CursorPage<RestaurantSummaryResponse>> res = restaurantController.listApproved(null, 20);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().content()).hasSize(1);
    }

    @Test
    @DisplayName("getRestaurant returns public restaurant details")
    void getRestaurant_success() {
        when(restaurantService.getRestaurant(restaurantId)).thenReturn(mockResponse);

        ResponseEntity<RestaurantResponse> res = restaurantController.getRestaurant(restaurantId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().name()).isEqualTo("Trattoria Mario");
    }

    @Test
    @DisplayName("findNearbyRestaurants returns list of restaurants within radius")
    void findNearbyRestaurants_success() {
        com.quickbite.quickbite.restaurant.dto.NearbyRestaurantResponse nearby =
                new com.quickbite.quickbite.restaurant.dto.NearbyRestaurantResponse(
                        restaurantId, "Trattoria Mario", BigDecimal.valueOf(4.8), 100L,
                        false, RestaurantVerificationStatus.APPROVED, Instant.now(), 1250.0
                );
        when(restaurantService.findNearbyRestaurants(12.9716, 77.5946, 5000, 0, 20))
                .thenReturn(List.of(nearby));

        ResponseEntity<List<com.quickbite.quickbite.restaurant.dto.NearbyRestaurantResponse>> res =
                restaurantController.findNearbyRestaurants(12.9716, 77.5946, 5000, 0, 20);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).hasSize(1);
        assertThat(res.getBody().getFirst().distanceMeters()).isEqualTo(1250.0);
    }

    @Test
    @DisplayName("listMyRestaurants delegates to service with owner ID from JWT")
    void listMyRestaurants_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);
        CursorPage<RestaurantSummaryResponse> page = CursorPage.of(List.of(mockSummary), 20, RestaurantSummaryResponse::id);
        when(restaurantService.listMyRestaurants(ownerId, RestaurantVerificationStatus.APPROVED, null, 20)).thenReturn(page);

        ResponseEntity<CursorPage<RestaurantSummaryResponse>> res = restaurantController.listMyRestaurants(
                jwt, RestaurantVerificationStatus.APPROVED, null, 20);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().content()).hasSize(1);
    }

    @Test
    @DisplayName("getMyRestaurant delegates to service")
    void getMyRestaurant_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);
        when(restaurantService.getMyRestaurant(restaurantId, ownerId)).thenReturn(mockResponse);

        ResponseEntity<RestaurantResponse> res = restaurantController.getMyRestaurant(jwt, restaurantId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().id()).isEqualTo(restaurantId);
    }

    @Test
    @DisplayName("updateRestaurant delegates to service")
    void updateRestaurant_success() {
        UpdateRestaurantRequest req = new UpdateRestaurantRequest("Updated Name", "Updated Desc");
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);
        when(restaurantService.update(restaurantId, ownerId, req)).thenReturn(mockResponse);

        ResponseEntity<RestaurantResponse> res = restaurantController.updateRestaurant(jwt, restaurantId, req);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(restaurantService).update(restaurantId, ownerId, req);
    }

    @Test
    @DisplayName("setRestaurantHours delegates to service")
    void setRestaurantHours_success() {
        List<RestaurantHoursRequest> hours = List.of(
                new RestaurantHoursRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(22, 0))
        );
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);
        when(restaurantService.setHours(restaurantId, ownerId, hours)).thenReturn(mockResponse);

        ResponseEntity<RestaurantResponse> res = restaurantController.setRestaurantHours(jwt, restaurantId, hours);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(restaurantService).setHours(restaurantId, ownerId, hours);
    }

    @Test
    @DisplayName("addRestaurantImage delegates to service")
    void addRestaurantImage_success() {
        RestaurantImageRequest req = new RestaurantImageRequest("https://img.com/food.jpg", 1);
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);
        when(restaurantService.addImage(restaurantId, ownerId, req.imageUrl(), req.displayOrder())).thenReturn(mockResponse);

        ResponseEntity<RestaurantResponse> res = restaurantController.addRestaurantImage(jwt, restaurantId, req);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(restaurantService).addImage(restaurantId, ownerId, req.imageUrl(), req.displayOrder());
    }

    @Test
    @DisplayName("removeRestaurantImage delegates to service")
    void removeRestaurantImage_success() {
        UUID imageId = UUID.randomUUID();
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);
        when(restaurantService.removeImage(restaurantId, ownerId, imageId)).thenReturn(mockResponse);

        ResponseEntity<RestaurantResponse> res = restaurantController.removeRestaurantImage(jwt, restaurantId, imageId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(restaurantService).removeImage(restaurantId, ownerId, imageId);
    }

    @Test
    @DisplayName("toggleClosed delegates to service")
    void toggleClosed_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);
        when(restaurantService.toggleClosed(restaurantId, ownerId)).thenReturn(mockResponse);

        ResponseEntity<RestaurantResponse> res = restaurantController.toggleClosed(jwt, restaurantId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(restaurantService).toggleClosed(restaurantId, ownerId);
    }
}
