package com.quickbite.quickbite.common.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration for the delivery agent assignment strategy.
 * Bound from {@code quickbite.assignment.*} in application properties.
 *
 * <p>Lives in {@code common/config} because it is infrastructure-level
 * configuration shared across the delivery domain, not internal strategy logic.
 */
@ConfigurationProperties(prefix = "quickbite.assignment")
public record AssignmentProperties(
        /*
         * Number of nearest agents retrieved by PostGIS before OSRM road-time ranking narrows the pool.
         * Higher values improve accuracy at the cost of more OSRM calls.
         */
        int candidatePoolSize,

        /*
         * Maximum acceptable road driving duration in seconds.
         * Agents whose estimated drive time exceeds this are excluded.
         */
        int maxAcceptableDriveSeconds
) {
    public AssignmentProperties {
        if (candidatePoolSize <= 0) {
            candidatePoolSize = 10;
        }
        if (maxAcceptableDriveSeconds <= 0) {
            maxAcceptableDriveSeconds = 900; // 15 minutes default
        }
    }
}
