package com.quickbite.quickbite.cart.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.cart.dto.CartResponse;
import com.quickbite.quickbite.cart.dto.CheckoutPreviewResponse;
import com.quickbite.quickbite.cart.service.CartCheckoutService;
import com.quickbite.quickbite.cart.service.CartService;
import com.quickbite.quickbite.common.dto.PriceBreakdown;
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
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock private CartService cartService;
    @Mock private CartCheckoutService cartCheckoutService;
    @Mock private AuthenticatedSessionResolver authenticatedSessionResolver;
    @Mock private Jwt jwt;

    @InjectMocks
    private CartController cartController;

    private UUID userId;
    private UUID addressId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        addressId = UUID.randomUUID();
    }

    @Test
    @DisplayName("getCheckoutPreview delegates to CartCheckoutService and returns 200 OK")
    void getCheckoutPreview_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);

        PriceBreakdown breakdown = new PriceBreakdown(
                BigDecimal.valueOf(200.00), BigDecimal.ZERO, BigDecimal.valueOf(30.00),
                BigDecimal.valueOf(5.00), BigDecimal.valueOf(10.00), BigDecimal.valueOf(10.00),
                BigDecimal.valueOf(255.00), 2000.0, 400L);

        CheckoutPreviewResponse preview = new CheckoutPreviewResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Pizza Planet",
                Collections.emptyList(), breakdown, Instant.now().plusSeconds(1800));

        when(cartCheckoutService.getCheckoutPreview(userId, addressId, BigDecimal.valueOf(10.00)))
                .thenReturn(preview);

        ResponseEntity<CheckoutPreviewResponse> response = cartController.getCheckoutPreview(
                jwt, addressId, BigDecimal.valueOf(10.00));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(preview);
        verify(cartCheckoutService).getCheckoutPreview(userId, addressId, BigDecimal.valueOf(10.00));
    }

    @Test
    @DisplayName("getCart delegates to CartService and returns 200 OK")
    void getCart_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);

        CartResponse cartResponse = new CartResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Pizza Planet",
                Collections.emptyList(), BigDecimal.valueOf(200.00), Instant.now().plusSeconds(1800));

        when(cartService.getCart(userId)).thenReturn(cartResponse);

        ResponseEntity<CartResponse> response = cartController.getCart(jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(cartResponse);
        verify(cartService).getCart(userId);
    }
}
