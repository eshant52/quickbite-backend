package com.quickbite.quickbite.dtos.auth;

import com.quickbite.quickbite.models.ClientType;

/**
 * Structured device information parsed server-side from the User-Agent header.
 * Replaces the raw client-sent deviceInfo string for better security and consistency.
 *
 * @param deviceName  Human-readable device or browser name, e.g. "Chrome 126" or "iPhone 15 Pro"
 * @param os          Operating system with version, e.g. "iOS 18.1" or "Windows 11"
 * @param clientType  Whether the client is a web browser or mobile app
 */
public record DeviceInfo(
        String deviceName,
        String os,
        ClientType clientType
) {
}
