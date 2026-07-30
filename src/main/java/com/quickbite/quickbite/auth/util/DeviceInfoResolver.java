package com.quickbite.quickbite.auth.util;

import com.quickbite.quickbite.auth.dto.ClientRequestMetadata;
import com.quickbite.quickbite.auth.dto.DeviceInfo;

/**
 * Resolves structured device information from transport-neutral request metadata.
 */
public interface DeviceInfoResolver {
    DeviceInfo resolve(ClientRequestMetadata requestMetadata);
}
