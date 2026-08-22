package com.quickbite.quickbite.cart.dto;

import com.quickbite.quickbite.cart.model.Cart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        UUID restaurantId,
        String restaurantName,
        List<CartItemResponse> items,
        BigDecimal totalPrice,
        Instant expiresAt) {

    public static CartResponse from(Cart cart) {
        return new CartResponse(
                cart.getId(),
                cart.getRestaurant().getId(),
                cart.getRestaurant().getName(),
                cart.getItems().stream().map(CartItemResponse::from).toList(),
                cart.getTotalPrice(),
                cart.getExpiresAt()
        );
    }
}
