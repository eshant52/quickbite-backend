package com.quickbite.quickbite.common.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for Redis Geospatial Delivery Driver Tracking.
 * Bound from {@code quickbite.delivery.geo.*} in application properties.
 */
@ConfigurationProperties(prefix = "quickbite.delivery.geo")
public record DeliveryGeoProperties(
        boolean enabled,
        Duration heartbeatTtl,
        Duration syncInterval
) {
    public DeliveryGeoProperties {
        if (heartbeatTtl == null || heartbeatTtl.isZero() || heartbeatTtl.isNegative()) {
            heartbeatTtl = Duration.ofSeconds(60);
        }
        if (syncInterval == null || syncInterval.isZero() || syncInterval.isNegative()) {
            syncInterval = Duration.ofSeconds(30);
        }
    }
}
