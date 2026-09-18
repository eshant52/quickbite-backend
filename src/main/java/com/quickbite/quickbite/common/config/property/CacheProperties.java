package com.quickbite.quickbite.common.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Typed configuration properties for Redis cache.
 *
 * <p>Binds to the {@code app.cache.*} prefix in {@code application.properties}.
 */
@ConfigurationProperties(prefix = "app.cache")
public record CacheProperties(
        Duration defaultTtl
) {
    public CacheProperties {
        if (defaultTtl == null) {
            defaultTtl = Duration.ofMinutes(10);
        }
    }
}
