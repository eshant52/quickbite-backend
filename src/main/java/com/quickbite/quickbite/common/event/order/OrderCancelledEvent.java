package com.quickbite.quickbite.common.event.order;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID orderId,
        UUID customerId,
        UUID restaurantId,
        Instant cancelledAt
) implements OrderEvent {}
