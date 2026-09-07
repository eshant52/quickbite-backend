package com.quickbite.quickbite.order.service.fee;

import com.quickbite.quickbite.common.config.property.DeliveryFeeProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Step 1: Adds the flat base delivery fee.
 * e.g. ₹15 regardless of distance.
 */
@Component
@Order(1)
public class BaseFeeCalculator implements DeliveryFeeCalculator {

    private final DeliveryFeeProperties properties;

    public BaseFeeCalculator(DeliveryFeeProperties properties) {
        this.properties = properties;
    }

    @Override
    public BigDecimal calculate(FeeContext context, BigDecimal currentFee) {
        return currentFee.add(properties.baseFee());
    }
}
