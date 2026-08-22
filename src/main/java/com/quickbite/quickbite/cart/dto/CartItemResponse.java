package com.quickbite.quickbite.cart.dto;

import com.quickbite.quickbite.cart.model.CartItem;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID menuItemId,
        String menuItemName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subTotal) {

    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(
                item.getId(),
                item.getMenuItem().getId(),
                item.getMenuItem().getName(),
                item.getQuantity(),
                item.getMenuItem().getPrice(),
                item.getSubTotal()
        );
    }
}
