package com.quickbite.quickbite.common.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Typed configuration for order pricing (platform fee and GST rate).
 * Bound from {@code quickbite.pricing.*} in application.properties.
 */
@ConfigurationProperties(prefix = "quickbite.pricing")
public record PricingProperties(
        BigDecimal platformFee,
        BigDecimal gstRate
) {
    public PricingProperties {
        if (platformFee == null) {
            platformFee = BigDecimal.valueOf(5.00);
        }
        if (gstRate == null) {
            gstRate = BigDecimal.valueOf(0.05);
        }
    }
}
