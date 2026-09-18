package com.quickbite.quickbite.common.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration properties for Razorpay payment gateway credentials.
 * Values are sourced from environment variables via application.properties:
 *   razorpay.key-id     → RAZORPAY_KEY_ID
 *   razorpay.key-secret → RAZORPAY_KEY_SECRET
 *   razorpay.webhook-secret → RAZORPAY_WEBHOOK_SECRET
 */
@ConfigurationProperties(prefix = "razorpay")
public record RazorpayProperties(
        String keyId,
        String keySecret,
        String webhookSecret
) {
}
