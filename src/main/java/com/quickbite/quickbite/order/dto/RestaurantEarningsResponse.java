package com.quickbite.quickbite.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RestaurantEarningsResponse(
        UUID restaurantId,
        long deliveredOrdersCount,
        BigDecimal totalGrossSales,
        BigDecimal totalFoodSubtotal,
        BigDecimal totalPlatformFees,
        BigDecimal totalTaxesCollected,
        Instant computedAt
) {
}
