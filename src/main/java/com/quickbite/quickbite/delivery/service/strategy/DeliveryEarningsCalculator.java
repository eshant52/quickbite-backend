package com.quickbite.quickbite.delivery.service.strategy;

import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.order.model.Order;

import java.math.BigDecimal;

/**
 * Strategy interface for calculating delivery agent payouts and earning summaries.
 */
public interface DeliveryEarningsCalculator {

    DeliveryEarningsBreakdown calculateEarnings(Order order, DeliveryAgent agent);

    record DeliveryEarningsBreakdown(
            BigDecimal guaranteedPayout,
            BigDecimal tipAmount,
            BigDecimal estimatedTotalPayout,
            Double deliveryDistanceKm,
            Long estimatedDeliverySeconds
    ) {}
}
