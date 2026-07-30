package com.quickbite.quickbite.auth.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.quickbite.quickbite.auth.dto.ClientRequestMetadata;
import com.quickbite.quickbite.auth.dto.DeviceInfo;
import com.quickbite.quickbite.auth.model.ClientType;
import com.quickbite.quickbite.common.exception.BadRequestException;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * YAUAA-backed {@link DeviceInfoResolver}. This adapter is the only auth component
 * that depends on the user-agent parsing library.
 */
@Component
public class YauaaDeviceInfoResolver implements DeviceInfoResolver {
    private static final String X_CLIENT_TYPE = "X-Client-Type";

    private final UserAgentAnalyzer userAgentAnalyzer;
    private final Cache<String, UserAgent> cache;

    public YauaaDeviceInfoResolver() {
        userAgentAnalyzer = UserAgentAnalyzer
                .newBuilder()
                .hideMatcherLoadStats()
                .withField(UserAgent.DEVICE_NAME)
                .withField(UserAgent.DEVICE_BRAND)
                .withField(UserAgent.OPERATING_SYSTEM_NAME)
                .withField(UserAgent.OPERATING_SYSTEM_VERSION)
                .withField(UserAgent.AGENT_NAME)
                .withField(UserAgent.AGENT_VERSION_MAJOR)
                .build();

        cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofHours(24))
                .build();
    }

    @Override
    public DeviceInfo resolve(ClientRequestMetadata requestMetadata) {
        ClientType clientType = resolveClientType(requestMetadata.clientType());
        String userAgentValue = requestMetadata.userAgent();

        if (userAgentValue == null || userAgentValue.isBlank()) {
            return new DeviceInfo("Unknown", "Unknown", "Unknown", clientType,
                    requestMetadata.ipAddress(), userAgentValue);
        }

        UserAgent userAgent = cache.get(userAgentValue, userAgentAnalyzer::parse);
        return new DeviceInfo(
                deviceName(userAgent),
                operatingSystem(userAgent),
                operatingSystemVersion(userAgent),
                clientType,
                requestMetadata.ipAddress(),
                userAgentValue);
    }

    private ClientType resolveClientType(String clientType) {
        if (clientType == null || clientType.isBlank()) {
            throw new BadRequestException("Missing required header " + X_CLIENT_TYPE
                    + ". Allowed values: 'mobile_app', 'web_browser'");
        }

        return switch (clientType.trim()) {
            case "mobile_app" -> ClientType.MOBILE_APP;
            case "web_browser" -> ClientType.WEB_BROWSER;
            default -> throw new BadRequestException("Invalid " + X_CLIENT_TYPE + " header value: '"
                    + clientType + "'. Allowed values: 'mobile_app', 'web_browser'");
        };
    }

    private String deviceName(UserAgent userAgent) {
        String deviceName = userAgent.getValue(UserAgent.DEVICE_NAME);
        String deviceBrand = userAgent.getValue(UserAgent.DEVICE_BRAND);
        if (known(deviceName)) {
            return known(deviceBrand) && !deviceName.startsWith(deviceBrand)
                    ? deviceBrand + " " + deviceName
                    : deviceName;
        }

        String agentName = userAgent.getValue(UserAgent.AGENT_NAME);
        String agentVersion = userAgent.getValue(UserAgent.AGENT_VERSION_MAJOR);
        if (known(agentName)) {
            return known(agentVersion) ? agentName + " " + agentVersion : agentName;
        }
        return "Unknown";
    }

    private String operatingSystem(UserAgent userAgent) {
        String operatingSystem = userAgent.getValue(UserAgent.OPERATING_SYSTEM_NAME);
        return known(operatingSystem) ? operatingSystem : "Unknown";
    }

    private String operatingSystemVersion(UserAgent userAgent) {
        String operatingSystemVersion = userAgent.getValue(UserAgent.OPERATING_SYSTEM_VERSION);
        return known(operatingSystemVersion) ? operatingSystemVersion : "Unknown";
    }

    private boolean known(String value) {
        return value != null && !value.isBlank() && !"Unknown".equalsIgnoreCase(value);
    }
}
