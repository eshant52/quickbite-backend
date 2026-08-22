package com.quickbite.quickbite.cart.service;

import com.quickbite.quickbite.cart.dto.AddCartItemRequest;
import com.quickbite.quickbite.cart.dto.CartResponse;
import com.quickbite.quickbite.cart.dto.UpdateCartItemRequest;

import java.util.UUID;

public interface CartService {
    CartResponse getCart(UUID customerId);
    CartResponse addItem(UUID customerId, AddCartItemRequest req);
    CartResponse updateItem(UUID customerId, UUID cartItemId, UpdateCartItemRequest req);
    void removeItem(UUID customerId, UUID cartItemId);
    void clearCart(UUID customerId);
}
