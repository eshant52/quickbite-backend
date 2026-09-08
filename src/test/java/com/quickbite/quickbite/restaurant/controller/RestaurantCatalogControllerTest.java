package com.quickbite.quickbite.restaurant.controller;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.restaurant.dto.NearbyRestaurantResponse;
import com.quickbite.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.quickbite.restaurant.dto.RestaurantSummaryResponse;
import com.quickbite.quickbite.restaurant.model.RestaurantVerificationStatus;
import com.quickbite.quickbite.restaurant.service.RestaurantCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantCatalogControllerTest {

    @Mock
    private RestaurantCatalogService restaurantCatalogService;

    @InjectMocks
    private RestaurantCatalogController restaurantCatalogController;

    private UUID restaurantId;
    private RestaurantResponse mockResponse;
    private RestaurantSummaryResponse mockSummary;

    @BeforeEach
    void setUp() {
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
                UUID.randomUUID(),
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
        when(restaurantCatalogService.listApproved(null, 20)).thenReturn(page);

        ResponseEntity<CursorPage<RestaurantSummaryResponse>> res = restaurantCatalogController.listApproved(null, 20);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().content()).hasSize(1);
    }

    @Test
    @DisplayName("getRestaurant returns public restaurant details")
    void getRestaurant_success() {
        when(restaurantCatalogService.getRestaurant(restaurantId)).thenReturn(mockResponse);

        ResponseEntity<RestaurantResponse> res = restaurantCatalogController.getRestaurant(restaurantId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().name()).isEqualTo("Trattoria Mario");
    }

    @Test
    @DisplayName("findNearbyRestaurants returns list of restaurants within radius")
    void findNearbyRestaurants_success() {
        NearbyRestaurantResponse nearby = new NearbyRestaurantResponse(
                restaurantId, "Trattoria Mario", BigDecimal.valueOf(4.8), 100L,
                false, RestaurantVerificationStatus.APPROVED, Instant.now(), 1250.0
        );
        when(restaurantCatalogService.findNearbyRestaurants(12.9716, 77.5946, 5000, 0, 20))
                .thenReturn(List.of(nearby));

        ResponseEntity<List<NearbyRestaurantResponse>> res =
                restaurantCatalogController.findNearbyRestaurants(12.9716, 77.5946, 5000, 0, 20);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).hasSize(1);
        assertThat(res.getBody().getFirst().distanceMeters()).isEqualTo(1250.0);
    }
}
