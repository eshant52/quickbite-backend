package com.quickbite.quickbite.order.service.fee;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Step 3: Placeholder for future surge pricing logic.
 *
 * <p>Currently returns ZERO — no surge applied.
 * When surge pricing is needed (e.g., peak hours, rain, festivals),
 * implement the logic here without modifying any other calculator.
 */
@Component
@Order(3)
public class SurgeFeeCalculator implements DeliveryFeeCalculator {

    @Override
    public BigDecimal calculate(FeeContext context, BigDecimal currentFee) {
        // No surge pricing yet — future: check time-of-day, weather, demand
        return currentFee;
    }
}
