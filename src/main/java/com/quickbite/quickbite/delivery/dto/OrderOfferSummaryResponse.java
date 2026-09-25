package com.quickbite.quickbite.delivery.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderOfferSummaryResponse(
        UUID orderId,
        String restaurantName,
        String restaurantAddress,
        String deliveryAddress,
        Double deliveryDistanceKm,
        Long estimatedDeliverySeconds,
        BigDecimal guaranteedPayout,
        BigDecimal tipAmount,
        BigDecimal estimatedTotalPayout,
        Instant offerExpiresAt,
        long secondsRemaining
) {}
