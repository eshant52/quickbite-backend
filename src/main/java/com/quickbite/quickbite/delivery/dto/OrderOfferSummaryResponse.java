package com.quickbite.quickbite.delivery.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderOfferSummaryResponse(
        UUID offerId,
        UUID orderId,
        int roundNumber,
        BigDecimal radiusKm,
        String orderStatus,
        BigDecimal orderSubtotal,
        BigDecimal orderTotalAmount,
        String customerName,
        String customerPhone,
        List<OfferOrderItemDto> items,
        String restaurantName,
        String restaurantAddress,
        String deliveryAddress,
        Double deliveryDistanceKm,
        Long estimatedDeliverySeconds,
        BigDecimal guaranteedPayout,
        BigDecimal tipAmount,
        BigDecimal estimatedTotalPayout,
        Instant offeredAt,
        Instant offerExpiresAt,
        long secondsRemaining
) {
    public record OfferOrderItemDto(
            String name,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subTotal
    ) {}
}
