package com.quickbite.quickbite.auth.dto;

import com.quickbite.quickbite.auth.model.ClientType;

/**
 * Structured device information parsed server-side from the User-Agent header.
 * Replaces the raw client-sent deviceInfo string for better security and consistency.
 *
 * @param deviceName  Human-readable device or browser name, e.g. "Chrome 126" or "iPhone 15 Pro"
 * @param deviceOs    Operating system with version, e.g. "iOS 18.1" or "Windows 11"
 * @param osVersion   Version of the operating system
 * @param clientType  Whether the client is a web browser or mobile app
 * @param ip          The IP address of the client making the request
 * @param userAgent   The raw User-Agent header string sent by the client
 */
public record DeviceInfo(
        String deviceName,
        String deviceOs,
        String osVersion,
        ClientType clientType,
        String ip,
        String userAgent
) {
}
