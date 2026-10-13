package com.quickbite.quickbite.common.dto;

import java.math.BigDecimal;

/**
 * Breakdown of all fees, discounts, and taxes for an order or checkout preview.
 */
public record PriceBreakdown(
        BigDecimal subTotal,
        BigDecimal discountAmount,
        BigDecimal deliveryFee,
        BigDecimal platformFee,
        BigDecimal taxAmount,
        BigDecimal tipAmount,
        BigDecimal totalAmount,
        Double deliveryDistanceMeters,
        Long estimatedDeliverySeconds
) {
}
