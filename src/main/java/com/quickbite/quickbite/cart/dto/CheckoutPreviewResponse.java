package com.quickbite.quickbite.cart.dto;

import com.quickbite.quickbite.cart.model.Cart;
import com.quickbite.quickbite.common.dto.PriceBreakdown;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only checkout preview response showing items, restaurant info, and the full price breakdown.
 */
public record CheckoutPreviewResponse(
        UUID cartId,
        UUID restaurantId,
        String restaurantName,
        List<CartItemResponse> items,
        PriceBreakdown priceBreakdown,
        Instant expiresAt
) {
    public static CheckoutPreviewResponse from(Cart cart, PriceBreakdown priceBreakdown) {
        return new CheckoutPreviewResponse(
                cart.getId(),
                cart.getRestaurant().getId(),
                cart.getRestaurant().getName(),
                cart.getItems().stream().map(CartItemResponse::from).toList(),
                priceBreakdown,
                cart.getExpiresAt()
        );
    }
}
