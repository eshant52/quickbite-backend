package com.quickbite.quickbite.restaurant.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.restaurant.dto.*;
import com.quickbite.quickbite.restaurant.model.RestaurantVerificationStatus;
import com.quickbite.quickbite.restaurant.service.RestaurantOwnerService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantOwnerControllerTest {

    @Mock
    private RestaurantOwnerService restaurantOwnerService;

    @Mock
    private AuthenticatedSessionResolver authenticatedSessionResolver;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private RestaurantOwnerController restaurantOwnerController;

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
    @DisplayName("listMyRestaurants delegates to service with owner ID from JWT")
    void listMyRestaurants_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);
        CursorPage<RestaurantSummaryResponse> page = CursorPage.of(List.of(mockSummary), 20, RestaurantSummaryResponse::id);
        when(restaurantOwnerService.listMyRestaurants(ownerId, RestaurantVerificationStatus.APPROVED, null, 20)).thenReturn(page);

        ResponseEntity<CursorPage<RestaurantSummaryResponse>> res = restaurantOwnerController.listMyRestaurants(
                jwt, RestaurantVerificationStatus.APPROVED, null, 20);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().content()).hasSize(1);
    }

    @Test
    @DisplayName("getMyRestaurant delegates to service")
    void getMyRestaurant_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);
        when(restaurantOwnerService.getMyRestaurant(restaurantId, ownerId)).thenReturn(mockResponse);

        ResponseEntity<RestaurantResponse> res = restaurantOwnerController.getMyRestaurant(jwt, restaurantId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().id()).isEqualTo(restaurantId);
    }

    @Test
    @DisplayName("update delegates to service")
    void update_success() {
        UpdateRestaurantRequest req = new UpdateRestaurantRequest("Updated Name", "Updated Desc");
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);
        when(restaurantOwnerService.update(restaurantId, ownerId, req)).thenReturn(mockResponse);

        ResponseEntity<RestaurantResponse> res = restaurantOwnerController.update(jwt, restaurantId, req);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(restaurantOwnerService).update(restaurantId, ownerId, req);
    }

    @Test
    @DisplayName("setHours delegates to service")
    void setHours_success() {
        List<RestaurantHoursRequest> hours = List.of(
                new RestaurantHoursRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(22, 0))
        );
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);
        when(restaurantOwnerService.setHours(restaurantId, ownerId, hours)).thenReturn(mockResponse);

        ResponseEntity<RestaurantResponse> res = restaurantOwnerController.setHours(jwt, restaurantId, hours);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(restaurantOwnerService).setHours(restaurantId, ownerId, hours);
    }

    @Test
    @DisplayName("addImage delegates to service")
    void addImage_success() {
        RestaurantImageRequest req = new RestaurantImageRequest("https://img.com/food.jpg", 1);
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);
        when(restaurantOwnerService.addImage(restaurantId, ownerId, req.imageUrl(), req.displayOrder())).thenReturn(mockResponse);

        ResponseEntity<RestaurantResponse> res = restaurantOwnerController.addImage(jwt, restaurantId, req);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(restaurantOwnerService).addImage(restaurantId, ownerId, req.imageUrl(), req.displayOrder());
    }

    @Test
    @DisplayName("removeImage delegates to service")
    void removeImage_success() {
        UUID imageId = UUID.randomUUID();
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);
        when(restaurantOwnerService.removeImage(restaurantId, ownerId, imageId)).thenReturn(mockResponse);

        ResponseEntity<RestaurantResponse> res = restaurantOwnerController.removeImage(jwt, restaurantId, imageId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(restaurantOwnerService).removeImage(restaurantId, ownerId, imageId);
    }

    @Test
    @DisplayName("toggleClosed delegates to service")
    void toggleClosed_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);
        when(restaurantOwnerService.toggleClosed(restaurantId, ownerId)).thenReturn(mockResponse);

        ResponseEntity<RestaurantResponse> res = restaurantOwnerController.toggleClosed(jwt, restaurantId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(restaurantOwnerService).toggleClosed(restaurantId, ownerId);
    }
}
