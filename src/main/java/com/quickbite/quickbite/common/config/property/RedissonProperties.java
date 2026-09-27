package com.quickbite.quickbite.common.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration properties for the Redisson client.
 *
 * <p>Binds to the {@code quickbite.redisson.*} prefix in {@code application.properties}.
 * Defaults match the project's local Redis configuration.
 *
 * <p>Example configuration:
 * <pre>
 * quickbite.redisson.host=localhost
 * quickbite.redisson.port=6377
 * quickbite.redisson.connection-pool-size=10
 * quickbite.redisson.connection-minimum-idle-size=2
 * quickbite.redisson.ssl=false
 * quickbite.redisson.username=
 * quickbite.redisson.password=
 * </pre>
 */
@ConfigurationProperties(prefix = "quickbite.redisson")
public record RedissonProperties(
        String host,
        int port,
        int connectionPoolSize,
        int connectionMinimumIdleSize,
        boolean ssl,
        String username,
        String password
) {
    public RedissonProperties {
        if (host == null || host.isBlank()) {
            host = "localhost";
        }
        if (port <= 0) {
            port = 6377;
        }
        if (connectionPoolSize <= 0) {
            connectionPoolSize = 10;
        }
        if (connectionMinimumIdleSize <= 0) {
            connectionMinimumIdleSize = 2;
        }
    }
}
