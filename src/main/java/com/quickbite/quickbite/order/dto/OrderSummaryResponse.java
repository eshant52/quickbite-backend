package com.quickbite.quickbite.order.dto;

import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String restaurantName,
        OrderStatus currentStatus,
        BigDecimal totalAmount,
        Instant createdAt
) {
    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getRestaurant().getName(),
                order.getCurrentStatus(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }
}
