package com.quickbite.quickbite.order.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.order.dto.RestaurantEarningsResponse;
import com.quickbite.quickbite.order.service.RestaurantOrderService;
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
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantOrderControllerTest {

    @Mock
    private RestaurantOrderService restaurantOrderService;

    @Mock
    private AuthenticatedSessionResolver authenticatedSessionResolver;

    @InjectMocks
    private RestaurantOrderController restaurantOrderController;

    private UUID restaurantId;
    private UUID ownerId;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        jwt = mock(Jwt.class);

        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(ownerId);
    }

    @Test
    @DisplayName("getEarnings returns restaurant delivered earnings summary (Requirement #19)")
    void getEarnings_success() {
        Instant now = Instant.now();
        RestaurantEarningsResponse expectedResponse = new RestaurantEarningsResponse(
                restaurantId,
                15L,
                BigDecimal.valueOf(12500.00),
                BigDecimal.valueOf(11250.00),
                BigDecimal.valueOf(1250.00),
                BigDecimal.valueOf(0.00),
                now
        );

        when(restaurantOrderService.getEarnings(restaurantId, ownerId)).thenReturn(expectedResponse);

        ResponseEntity<RestaurantEarningsResponse> response = restaurantOrderController.getEarnings(jwt, restaurantId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().deliveredOrdersCount()).isEqualTo(15L);
        assertThat(response.getBody().totalGrossSales()).isEqualTo(BigDecimal.valueOf(12500.00));
        assertThat(response.getBody().totalFoodSubtotal()).isEqualTo(BigDecimal.valueOf(11250.00));
        assertThat(response.getBody().computedAt()).isEqualTo(now);
    }
}
