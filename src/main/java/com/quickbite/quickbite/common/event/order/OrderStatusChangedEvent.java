package com.quickbite.quickbite.common.event.order;

import com.quickbite.quickbite.order.model.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID orderId,
        UUID customerId,
        UUID restaurantId,
        OrderStatus previousStatus,
        OrderStatus newStatus,
        Instant changedAt
) implements OrderEvent {}
