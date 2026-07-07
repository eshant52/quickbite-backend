package com.quickbite.quickbite.utils;

import com.quickbite.quickbite.dtos.auth.DeviceInfo;
import com.quickbite.quickbite.models.ClientType;
import com.quickbite.quickbite.exceptions.BadRequestException;
import org.springframework.stereotype.Component;
import ua_parser.Client;
import ua_parser.Parser;

/**
 * Parses User-Agent headers into structured {@link DeviceInfo}.
 * <p>
 * Client type detection priority:
 * <ol>
 *   <li>Explicit {@code X-Client-Type} header (if the client sends one)</li>
 *   <li>User-Agent heuristics: if a known browser family is detected → WEB_BROWSER, otherwise MOBILE_APP</li>
 * </ol>
 */
@Component
public class UserAgentParser {
    private final Parser parser = new Parser();

    /**
     * Parse User-Agent into structured device info.
     *
     * @param userAgent    the raw User-Agent header value
     * @param clientTypeHeader required X-Client-Type header ("web_browser" or "mobile_app")
     * @return structured device info
     */
    public DeviceInfo parse(String userAgent, String clientTypeHeader) {
        // Enforce strict X-Client-Type header presence and values for this project.
        if (clientTypeHeader == null || clientTypeHeader.isBlank()) {
            throw new BadRequestException("Missing required header 'X-Client-Type'. Allowed values: 'mobile_app', 'web_browser'");
        }

        if (userAgent == null || userAgent.isBlank()) {
            return new DeviceInfo("Unknown", "Unknown", resolveClientType(null, clientTypeHeader));
        }

        Client client = parser.parse(userAgent);

        String deviceName = buildDeviceName(client);
        String os = buildOs(client);
        ClientType clientType = resolveClientType(client, clientTypeHeader);

        return new DeviceInfo(deviceName, os, clientType);
    }

    private String buildDeviceName(Client client) {
        if (client.device != null && client.device.family != null
                && !"Other".equalsIgnoreCase(client.device.family)) {
            // Physical device name available (e.g. "iPhone", "Samsung Galaxy S24")
            return client.device.family;
        }
        // Fall back to browser/user-agent family (e.g. "Chrome", "Firefox")
        if (client.userAgent != null && client.userAgent.family != null) {
            String name = client.userAgent.family;
            if (client.userAgent.major != null) {
                name += " " + client.userAgent.major;
            }
            return name;
        }
        return "Unknown";
    }

    private String buildOs(Client client) {
        if (client.os == null || client.os.family == null) {
            return "Unknown";
        }
        String os = client.os.family;
        if (client.os.major != null) {
            os += " " + client.os.major;
            if (client.os.minor != null) {
                os += "." + client.os.minor;
            }
        }
        return os;
    }

    private ClientType resolveClientType(Client client, String clientTypeHeader) {
        // 1. Explicit header takes precedence
        // Strict header mapping: only accept exact values
        if (clientTypeHeader != null && !clientTypeHeader.isBlank()) {
            String normalized = clientTypeHeader.trim();
            return switch (normalized) {
                case "mobile_app" -> ClientType.MOBILE_APP;
                case "web_browser" -> ClientType.WEB_BROWSER;
                default -> throw new BadRequestException("Invalid X-Client-Type header value: '" + clientTypeHeader + "'. Allowed values: 'mobile_app', 'web_browser'");
            };
        }

        // 2. Heuristic: if ua-parser detects a known browser family → WEB_BROWSER
        if (client != null && client.userAgent != null && client.userAgent.family != null) {
            String family = client.userAgent.family.toLowerCase();
            if (family.contains("chrome") || family.contains("firefox")
                    || family.contains("safari") || family.contains("edge")
                    || family.contains("opera") || family.contains("ie")
                    || family.contains("brave") || family.contains("vivaldi")
                    || family.contains("samsung") || family.contains("uc browser")) {
                return ClientType.WEB_BROWSER;
            }
        }
        return ClientType.MOBILE_APP;
    }
}
