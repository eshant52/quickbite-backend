package com.quickbite.quickbite.order.dto;

import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.model.OrderStatusHistory;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusHistoryDto(
        UUID id,
        OrderStatus status,
        Instant timestamp
) {
    public static OrderStatusHistoryDto from(OrderStatusHistory history) {
        return new OrderStatusHistoryDto(
                history.getId(),
                history.getOrderStatus(),
                history.getCreatedAt()
        );
    }
}
