package com.quickbite.quickbite.common.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

import java.util.List;

@ConfigurationProperties(prefix = "quickbite.auth")
public record AuthProperties(
        int maxConcurrentSessions,
        CookieProperties cookie,
        JwtProperties jwt,
        CorsProperties cors
) {

    public record CorsProperties(
            List<String> allowedOrigins
    ) {
    }

    public record CookieProperties(
            String refreshTokenName,
            String refreshTokenPath
    ) {
    }

    public record JwtProperties(
            String privateKey,
            String publicKey,
            Duration accessTokenExpiry,
            String accessTokenAudience,
            Duration refreshTokenExpiry,
            Duration challengeTokenExpiry,
            String challengeTokenAudience
    ) {
    }
}
