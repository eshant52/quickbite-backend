package com.quickbite.quickbite.common.event.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID orderId,
        UUID customerId,
        String customerName,
        String customerEmail,
        UUID restaurantId,
        String restaurantName,
        BigDecimal totalAmount,
        Instant placedAt
) implements OrderEvent {}
