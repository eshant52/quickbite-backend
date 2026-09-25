package com.quickbite.quickbite.common.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Typed configuration properties for the order domain.
 * Bound from {@code quickbite.order.*} in application properties.
 */
@ConfigurationProperties(prefix = "quickbite.order")
public record OrderProperties(
        /*
         * TTL in minutes after which an order in AWAITING_PAYMENT status is marked ABANDONED.
         */
        int abandonTtlMinutes,

        /*
         * Number of stale orders fetched and processed per batch during the abandonment sweep.
         */
        int abandonBatchSize,

        /*
         * Cron expression for the background abandonment sweep.
         */
        String abandonCron,

        /*
         * Cooldown duration in seconds to prevent accidental duplicate place-order submissions.
         */
        int placeCooldownSeconds,

        /*
         * Acceptance window for the restaurant after order is placed.
         */
        Duration restaurantAcceptanceWindow
) {
    public OrderProperties {
        if (abandonTtlMinutes <= 0) {
            abandonTtlMinutes = 15;
        }
        if (abandonBatchSize <= 0) {
            abandonBatchSize = 100;
        }
        if (abandonCron == null || abandonCron.isBlank()) {
            abandonCron = "0 */5 * * * *";
        }
        if (placeCooldownSeconds <= 0) {
            placeCooldownSeconds = 5;
        }
        if (restaurantAcceptanceWindow == null || restaurantAcceptanceWindow.isZero() || restaurantAcceptanceWindow.isNegative()) {
            restaurantAcceptanceWindow = Duration.ofMinutes(2);
        }
    }

    public OrderProperties(int abandonTtlMinutes, int abandonBatchSize, String abandonCron, int placeCooldownSeconds) {
        this(abandonTtlMinutes, abandonBatchSize, abandonCron, placeCooldownSeconds, Duration.ofMinutes(2));
    }

    public OrderProperties(int abandonTtlMinutes, int abandonBatchSize) {
        this(abandonTtlMinutes, abandonBatchSize, "0 */5 * * * *", 5, Duration.ofMinutes(2));
    }
}
