package com.quickbite.quickbite.order.dto;

import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.model.OrderStatusHistory;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusHistoryDto(
        UUID id,
        OrderStatus status,
        Instant timestamp,
        UUID changedById,
        String changedByName
) {
    public OrderStatusHistoryDto(UUID id, OrderStatus status, Instant timestamp) {
        this(id, status, timestamp, null, null);
    }

    public static OrderStatusHistoryDto from(OrderStatusHistory history) {
        return new OrderStatusHistoryDto(
                history.getId(),
                history.getOrderStatus(),
                history.getCreatedAt(),
                history.getChangedBy() != null ? history.getChangedBy().getId() : null,
                history.getChangedBy() != null ? history.getChangedBy().getName() : null
        );
    }
}
