package com.quickbite.quickbite.delivery.service.strategy;

import com.quickbite.quickbite.common.config.property.DispatchProperties;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.order.model.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ConfiguredDeliveryEarningsCalculator implements DeliveryEarningsCalculator {

    private final DispatchProperties dispatchProperties;

    public ConfiguredDeliveryEarningsCalculator(DispatchProperties dispatchProperties) {
        this.dispatchProperties = dispatchProperties;
    }

    @Override
    public DeliveryEarningsBreakdown calculateEarnings(Order order, DeliveryAgent agent) {
        double distanceKm = (order.getDeliveryDistanceMeters() != null)
                ? order.getDeliveryDistanceMeters() / 1000.0
                : 0.0;
        long durationSeconds = (order.getEstimatedDeliverySeconds() != null)
                ? order.getEstimatedDeliverySeconds()
                : 0L;

        BigDecimal base = dispatchProperties.earnings().basePayout();
        BigDecimal rate = dispatchProperties.earnings().ratePerKm();
        BigDecimal distanceBonus = rate.multiply(BigDecimal.valueOf(distanceKm)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal guaranteedPayout = base.add(distanceBonus).setScale(2, RoundingMode.HALF_UP);

        BigDecimal tip = (order.getTipAmount() != null)
                ? order.getTipAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal estimatedTotal = guaranteedPayout.add(tip).setScale(2, RoundingMode.HALF_UP);

        return new DeliveryEarningsBreakdown(
                guaranteedPayout,
                tip,
                estimatedTotal,
                distanceKm,
                durationSeconds
        );
    }
}
