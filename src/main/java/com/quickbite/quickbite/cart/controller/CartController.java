package com.quickbite.quickbite.cart.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.cart.dto.AddCartItemRequest;
import com.quickbite.quickbite.cart.dto.CartResponse;
import com.quickbite.quickbite.cart.dto.CheckoutPreviewResponse;
import com.quickbite.quickbite.cart.dto.UpdateCartItemRequest;
import com.quickbite.quickbite.cart.service.CartCheckoutService;
import com.quickbite.quickbite.cart.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {
    private final CartService cartService;
    private final CartCheckoutService cartCheckoutService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public CartController(
            CartService cartService,
            CartCheckoutService cartCheckoutService,
            AuthenticatedSessionResolver authenticatedSessionResolver) {
        this.cartService = cartService;
        this.cartCheckoutService = cartCheckoutService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(cartService.getCart(authenticatedSessionResolver.userIdFromJwt(jwt)));
    }

    @GetMapping("/checkout-preview")
    public ResponseEntity<CheckoutPreviewResponse> getCheckoutPreview(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID addressId,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal tipAmount) {
        return ResponseEntity.ok(cartCheckoutService.getCheckoutPreview(
                authenticatedSessionResolver.userIdFromJwt(jwt), addressId, tipAmount));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid AddCartItemRequest req) {
        return ResponseEntity.ok(cartService.addItem(authenticatedSessionResolver.userIdFromJwt(jwt), req));
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID cartItemId,
            @RequestBody @Valid UpdateCartItemRequest req) {
        return ResponseEntity.ok(cartService.updateItem(authenticatedSessionResolver.userIdFromJwt(jwt),
                cartItemId, req));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> removeItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID cartItemId) {
        cartService.removeItem(authenticatedSessionResolver.userIdFromJwt(jwt), cartItemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal Jwt jwt) {
        cartService.clearCart(authenticatedSessionResolver.userIdFromJwt(jwt));
        return ResponseEntity.noContent().build();
    }
}