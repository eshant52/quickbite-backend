package com.quickbite.quickbite.order.service.fee;

import java.math.BigDecimal;

/**
 * Single step in the delivery fee calculation chain (Chain of Responsibility).
 *
 * <p>Each implementation adds its component to the running total.
 * The {@code next} reference forms the chain; the last node has {@code next = null}.
 */
public interface DeliveryFeeCalculator {

    /**
     * Compute this step's fee contribution and add it to {@code currentFee}.
     *
     * @param context    routing and location data for this order
     * @param currentFee running total so far (starts at ZERO for the first node)
     * @return updated running total after adding this step's contribution
     */
    BigDecimal calculate(FeeContext context, BigDecimal currentFee);
}
