package com.quickbite.quickbite.auth.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.quickbite.quickbite.auth.dto.DeviceInfo;
import com.quickbite.quickbite.auth.model.ClientType;
import com.quickbite.quickbite.common.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Parses User-Agent headers into structured {@link DeviceInfo} using the Yauaa library
 * wrapped with a high-performance Caffeine cache.
 * <p>
 * Client type detection priority:
 * <ol>
 *   <li>Explicit {@code X-Client-Type} header (if the client sends one)</li>
 *   <li>User-Agent heuristics via Yauaa device class and agent name</li>
 * </ol>
 */
@Component
public class UserAgentParser {
    private static final String X_CLIENT_TYPE = "X-Client-Type";

    private final UserAgentAnalyzer uaa;
    private final Cache<String, UserAgent> cache;

    public UserAgentParser() {
        // Build Yauaa analyzer without internal cache and restricted only to fields we extract
        this.uaa = UserAgentAnalyzer
                .newBuilder()
                .hideMatcherLoadStats()
                .withField(UserAgent.DEVICE_NAME)
                .withField(UserAgent.DEVICE_BRAND)
                .withField(UserAgent.OPERATING_SYSTEM_NAME)
                .withField(UserAgent.OPERATING_SYSTEM_VERSION)
                .withField(UserAgent.AGENT_NAME)
                .withField(UserAgent.AGENT_VERSION_MAJOR)
                .withField(UserAgent.DEVICE_CLASS)
                .build();

        // High-performance Caffeine cache with W-TinyLFU eviction policy
        this.cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofHours(24))
                .build();
    }

    /**
     * Parse User-Agent and request headers into structured device info.
     *
     * @param request the incoming HTTP request
     * @return structured device info
     */
    public DeviceInfo parse(HttpServletRequest request) {
        String clientTypeHeader = request.getHeader(X_CLIENT_TYPE);
        String userAgentString = request.getHeader(HttpHeaders.USER_AGENT);
        String ip = extractIpAddress(request);

        // Enforce strict X-Client-Type header presence and values for this project.
        if (clientTypeHeader == null || clientTypeHeader.isBlank()) {
            throw new BadRequestException("Missing required header " + X_CLIENT_TYPE + ". Allowed values: 'mobile_app', 'web_browser'");
        }

        if (userAgentString == null || userAgentString.isBlank()) {
            return new DeviceInfo("Unknown", "Unknown", "Unknown", resolveClientType(null, clientTypeHeader), ip, userAgentString);
        }

        // Fast atomic read-through from Caffeine cache
        UserAgent userAgent = cache.get(userAgentString, uaa::parse);

        String deviceName = buildDeviceName(userAgent);
        String os = buildOs(userAgent);
        String osVersion = buildOsVersion(userAgent);
        ClientType clientType = resolveClientType(userAgent, clientTypeHeader);

        return new DeviceInfo(deviceName, os, osVersion, clientType, ip, userAgentString);
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String buildDeviceName(UserAgent agent) {
        String deviceName = agent.getValue(UserAgent.DEVICE_NAME);
        String deviceBrand = agent.getValue(UserAgent.DEVICE_BRAND);

        if (deviceName != null && !deviceName.isBlank() && !"Unknown".equalsIgnoreCase(deviceName)) {
            if (deviceBrand != null && !deviceBrand.isBlank() && !"Unknown".equalsIgnoreCase(deviceBrand) && !deviceName.startsWith(deviceBrand)) {
                return deviceBrand + " " + deviceName;
            }
            return deviceName;
        }

        String agentName = agent.getValue(UserAgent.AGENT_NAME);
        String agentVersion = agent.getValue(UserAgent.AGENT_VERSION_MAJOR);
        if (agentName != null && !agentName.isBlank() && !"Unknown".equalsIgnoreCase(agentName)) {
            if (agentVersion != null && !agentVersion.isBlank() && !"Unknown".equalsIgnoreCase(agentVersion)) {
                return agentName + " " + agentVersion;
            }
            return agentName;
        }
        return "Unknown";
    }

    private String buildOs(UserAgent agent) {
        String osName = agent.getValue(UserAgent.OPERATING_SYSTEM_NAME);
        if (osName == null || osName.isBlank()) {
            return "Unknown";
        }
        return osName;
    }

    private String buildOsVersion(UserAgent agent) {
        String osVersion = agent.getValue(UserAgent.OPERATING_SYSTEM_VERSION);
        if (osVersion == null || osVersion.isBlank()) {
            return "Unknown";
        }
        return osVersion;
    }

    private ClientType resolveClientType(UserAgent agent, String clientTypeHeader) {
        // 1. Explicit header takes precedence
        if (clientTypeHeader != null && !clientTypeHeader.isBlank()) {
            String normalized = clientTypeHeader.trim();
            return switch (normalized) {
                case "mobile_app" -> ClientType.MOBILE_APP;
                case "web_browser" -> ClientType.WEB_BROWSER;
                default -> throw new BadRequestException("Invalid " + X_CLIENT_TYPE + " header value: '" + clientTypeHeader + "'. Allowed values: 'mobile_app', 'web_browser'");
            };
        }

        // 2. Heuristic via Yauaa device class
        if (agent != null) {
            String deviceClass = agent.getValue(UserAgent.DEVICE_CLASS);
            if ("Desktop".equalsIgnoreCase(deviceClass) || "Anonymized".equalsIgnoreCase(deviceClass)) {
                return ClientType.WEB_BROWSER;
            }
        }
        return ClientType.MOBILE_APP;
    }
}
