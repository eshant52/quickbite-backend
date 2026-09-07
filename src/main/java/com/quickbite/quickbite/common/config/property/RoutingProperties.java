package com.quickbite.quickbite.common.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Typed configuration for all routing-related settings.
 *
 * <p>Bound from {@code quickbite.routing.*} in application properties.
 */
@ConfigurationProperties(prefix = "quickbite.routing")
public record RoutingProperties(
        boolean osrmEnabled,
        String osrmBaseUrl,
        boolean mapboxEnabled,
        String mapboxAccessToken,
        Duration timeout
) {
    public RoutingProperties {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            timeout = Duration.ofSeconds(3);
        }
    }
}
