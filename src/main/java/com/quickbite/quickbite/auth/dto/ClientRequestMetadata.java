package com.quickbite.quickbite.auth.dto;

/**
 * Request data needed to resolve the device that initiated an authentication action.
 */
public record ClientRequestMetadata(
        String userAgent,
        String clientType,
        String ipAddress
) {
}
