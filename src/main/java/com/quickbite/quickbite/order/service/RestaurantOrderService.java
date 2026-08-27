package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.order.dto.OrderResponse;
import com.quickbite.quickbite.order.dto.OrderSummaryResponse;
import com.quickbite.quickbite.order.model.OrderStatus;

import java.util.UUID;

public interface RestaurantOrderService {
    // Queries
    CursorPage<OrderSummaryResponse> listRestaurantOrders(UUID restaurantId, UUID ownerId, OrderStatus status, UUID cursor, int size);
    OrderResponse getRestaurantOrder(UUID orderId, UUID restaurantId, UUID ownerId);

    // State transitions
    OrderResponse acceptOrder(UUID orderId, UUID restaurantId, UUID ownerId);
    OrderResponse declineOrder(UUID orderId, UUID restaurantId, UUID ownerId);
    OrderResponse markPreparing(UUID orderId, UUID restaurantId, UUID ownerId);
    OrderResponse markReadyForPickup(UUID orderId, UUID restaurantId, UUID ownerId);
}
