package com.quickbite.quickbite.order.service.fee;

import com.quickbite.quickbite.common.config.property.DeliveryFeeProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Step 2: Adds a per-kilometre charge based on the OSRM road distance.
 * e.g. ₹8 × 4.2 km = ₹33.6.
 *
 * <p>If OSRM is unavailable and Haversine was used as fallback, the distance
 * is still a reasonable estimate (1.35× straight-line road factor applied).
 */
@Component
@Order(2)
public class DistanceFeeCalculator implements DeliveryFeeCalculator {

    private final DeliveryFeeProperties properties;

    public DistanceFeeCalculator(DeliveryFeeProperties properties) {
        this.properties = properties;
    }

    @Override
    public BigDecimal calculate(FeeContext context, BigDecimal currentFee) {
        double distanceKm = context.route().distanceMeters() / 1000.0;
        BigDecimal distanceFee = properties.ratePerKm()
                .multiply(BigDecimal.valueOf(distanceKm))
                .setScale(2, RoundingMode.HALF_UP);
        return currentFee.add(distanceFee);
    }
}
