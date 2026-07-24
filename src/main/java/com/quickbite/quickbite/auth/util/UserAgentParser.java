package com.quickbite.quickbite.auth.util;

import com.quickbite.quickbite.auth.dto.DeviceInfo;
import com.quickbite.quickbite.auth.model.ClientType;
import com.quickbite.quickbite.common.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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
    private static final String X_CLIENT_TYPE = "X-Client-Type";

    private final Parser parser;

    public UserAgentParser() {
        this.parser = new Parser();
    }

    /**
     * Parse User-Agent and request headers into structured device info.
     *
     * @param request the incoming HTTP request
     * @return structured device info
     */
    public DeviceInfo parse(HttpServletRequest request) {
        String clientTypeHeader = request.getHeader(X_CLIENT_TYPE);
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        String ip = extractIpAddress(request);

        // Enforce strict X-Client-Type header presence and values for this project.
        if (clientTypeHeader == null || clientTypeHeader.isBlank()) {
            throw new BadRequestException("Missing required header " + X_CLIENT_TYPE + ". Allowed values: 'mobile_app', 'web_browser'");
        }

        if (userAgent == null || userAgent.isBlank()) {
            return new DeviceInfo("Unknown", "Unknown", "Unknown", resolveClientType(null, clientTypeHeader), ip, userAgent);
        }

        Client client = parser.parse(userAgent);

        String deviceName = buildDeviceName(client);
        String os = buildOs(client);
        String osVersion = buildOsVersion(client);
        ClientType clientType = resolveClientType(client, clientTypeHeader);

        return new DeviceInfo(deviceName, os, osVersion, clientType, ip, userAgent);
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
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
        return client.os.family;
    }

    private String buildOsVersion(Client client) {
        if (client.os == null || client.os.major == null) {
            return "Unknown";
        }
        String version = client.os.major;
        if (client.os.minor != null) {
            version += "." + client.os.minor;
            if (client.os.patch != null && !client.os.patch.isBlank()) {
                version += "." + client.os.patch;
            }
        }
        return version;
    }

    private ClientType resolveClientType(Client client, String clientTypeHeader) {
        // 1. Explicit header takes precedence
        // Strict header mapping: only accept exact values
        if (clientTypeHeader != null && !clientTypeHeader.isBlank()) {
            String normalized = clientTypeHeader.trim();
            return switch (normalized) {
                case "mobile_app" -> ClientType.MOBILE_APP;
                case "web_browser" -> ClientType.WEB_BROWSER;
                default -> throw new BadRequestException("Invalid " + X_CLIENT_TYPE + " header value: '" + clientTypeHeader + "'. Allowed values: 'mobile_app', 'web_browser'");
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
