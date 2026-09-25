package com.quickbite.quickbite.common.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/**
 * Configuration properties for proactive delivery dispatch.
 * Bound from {@code quickbite.dispatch.*} in application properties.
 */
@ConfigurationProperties(prefix = "quickbite.dispatch")
public record DispatchProperties(
        List<DispatchRoundProperties> rounds,
        Duration pollInterval,
        Duration retryInterval,
        Duration driverOfferTimeout,
        int candidateLimit,
        EarningsProperties earnings
) {
    public DispatchProperties {
        if (rounds == null || rounds.isEmpty()) {
            rounds = List.of(
                    new DispatchRoundProperties(3.0, Duration.ofMinutes(2)),
                    new DispatchRoundProperties(6.0, Duration.ofMinutes(3)),
                    new DispatchRoundProperties(10.0, Duration.ofMinutes(5))
            );
        }
        if (pollInterval == null || pollInterval.isZero() || pollInterval.isNegative()) {
            pollInterval = Duration.ofSeconds(10);
        }
        if (retryInterval == null || retryInterval.isZero() || retryInterval.isNegative()) {
            retryInterval = Duration.ofSeconds(10);
        }
        if (driverOfferTimeout == null || driverOfferTimeout.isZero() || driverOfferTimeout.isNegative()) {
            driverOfferTimeout = Duration.ofSeconds(45);
        }
        if (candidateLimit <= 0) {
            candidateLimit = 10;
        }
        if (earnings == null) {
            earnings = new EarningsProperties(new BigDecimal("30.00"), new BigDecimal("10.00"));
        }
    }

    public record DispatchRoundProperties(
            double radiusKm,
            Duration roundDuration
    ) {
        public DispatchRoundProperties {
            if (radiusKm <= 0) {
                radiusKm = 3.0;
            }
            if (roundDuration == null || roundDuration.isZero() || roundDuration.isNegative()) {
                roundDuration = Duration.ofMinutes(2);
            }
        }
    }

    public record EarningsProperties(
            BigDecimal basePayout,
            BigDecimal ratePerKm
    ) {
        public EarningsProperties {
            if (basePayout == null || basePayout.compareTo(BigDecimal.ZERO) < 0) {
                basePayout = new BigDecimal("30.00");
            }
            if (ratePerKm == null || ratePerKm.compareTo(BigDecimal.ZERO) < 0) {
                ratePerKm = new BigDecimal("10.00");
            }
        }
    }
}
