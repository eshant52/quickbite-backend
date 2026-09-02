package com.quickbite.quickbite.delivery.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.quickbite.delivery.dto.UpdateLocationRequest;
import com.quickbite.quickbite.delivery.model.DeliveryAgentVerificationStatus;
import com.quickbite.quickbite.delivery.service.DeliveryService;
import com.quickbite.quickbite.order.dto.OrderResponse;
import com.quickbite.quickbite.order.model.OrderStatus;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryAgentControllerTest {

    @Mock
    private DeliveryService deliveryService;

    @Mock
    private AuthenticatedSessionResolver authenticatedSessionResolver;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private DeliveryAgentController deliveryAgentController;

    private UUID userId;
    private UUID orderId;
    private DeliveryAgentResponse mockAgentResponse;
    private OrderResponse mockOrderResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        mockAgentResponse = new DeliveryAgentResponse(
                UUID.randomUUID(),
                userId,
                "Rider Bob",
                "bob@delivery.com",
                "9876543210",
                true,
                DeliveryAgentVerificationStatus.APPROVED,
                12.9716,
                77.5946,
                Instant.now()
        );

        mockOrderResponse = new OrderResponse(
                orderId,
                UUID.randomUUID(),
                "Tasty Bites",
                "Street 42",
                List.of(),
                BigDecimal.valueOf(200),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(0),
                BigDecimal.valueOf(230),
                OrderStatus.OUT_FOR_DELIVERY,
                Instant.now()
        );
    }

    @Test
    @DisplayName("getProfile returns agent profile")
    void getProfile_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);
        when(deliveryService.getMyProfile(userId)).thenReturn(mockAgentResponse);

        ResponseEntity<DeliveryAgentResponse> res = deliveryAgentController.getProfile(jwt);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(mockAgentResponse);
    }

    @Test
    @DisplayName("updateLocation updates coordinates")
    void updateLocation_success() {
        UpdateLocationRequest req = new UpdateLocationRequest(12.9716, 77.5946);
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);
        when(deliveryService.updateLocation(userId, req)).thenReturn(mockAgentResponse);

        ResponseEntity<DeliveryAgentResponse> res = deliveryAgentController.updateLocation(jwt, req);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(deliveryService).updateLocation(userId, req);
    }

    @Test
    @DisplayName("markOutForDelivery marks order OUT_FOR_DELIVERY")
    void markOutForDelivery_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);
        when(deliveryService.markOutForDelivery(orderId, userId)).thenReturn(mockOrderResponse);

        ResponseEntity<OrderResponse> res = deliveryAgentController.markOutForDelivery(jwt, orderId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().currentStatus()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);
    }

    @Test
    @DisplayName("markDelivered marks order DELIVERED")
    void markDelivered_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);
        OrderResponse deliveredResponse = new OrderResponse(
                orderId, UUID.randomUUID(), "Tasty Bites", "Street 42", List.of(),
                BigDecimal.valueOf(200), BigDecimal.valueOf(20), BigDecimal.valueOf(10),
                BigDecimal.valueOf(0), BigDecimal.valueOf(230), OrderStatus.DELIVERED, Instant.now()
        );
        when(deliveryService.markDelivered(orderId, userId)).thenReturn(deliveredResponse);

        ResponseEntity<OrderResponse> res = deliveryAgentController.markDelivered(jwt, orderId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().currentStatus()).isEqualTo(OrderStatus.DELIVERED);
    }
}
