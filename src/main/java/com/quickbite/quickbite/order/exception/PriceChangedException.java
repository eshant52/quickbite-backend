package com.quickbite.quickbite.order.exception;

import com.quickbite.quickbite.common.dto.PriceBreakdown;

import java.math.BigDecimal;

/**
 * Thrown when an order placement specifies an expectedTotal that does not match
 * the freshly calculated total.
 */
public class PriceChangedException extends RuntimeException {

    private final BigDecimal currentTotal;
    private final BigDecimal expectedTotal;
    private final PriceBreakdown currentBreakdown;

    public PriceChangedException(BigDecimal currentTotal, BigDecimal expectedTotal, PriceBreakdown currentBreakdown) {
        super(String.format("Price has changed from %s to %s. Please review the updated total before proceeding.",
                expectedTotal, currentTotal));
        this.currentTotal = currentTotal;
        this.expectedTotal = expectedTotal;
        this.currentBreakdown = currentBreakdown;
    }

    public BigDecimal getCurrentTotal() {
        return currentTotal;
    }

    public BigDecimal getExpectedTotal() {
        return expectedTotal;
    }

    public PriceBreakdown getCurrentBreakdown() {
        return currentBreakdown;
    }
}
