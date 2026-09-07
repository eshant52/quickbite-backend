package com.quickbite.quickbite.common.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.math.BigDecimal;

/**
 * Typed configuration for delivery fee calculation.
 * Bound from {@code quickbite.fee.*} in application.properties.
 */
@ConfigurationProperties(prefix = "quickbite.fee")
public record DeliveryFeeProperties(
        BigDecimal baseFee,
        BigDecimal ratePerKm,
        BigDecimal minFee,
        BigDecimal maxFee
) {}
