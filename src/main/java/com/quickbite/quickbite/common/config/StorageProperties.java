package com.quickbite.quickbite.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Typed configuration for S3 storage.
 * Populated from {@code quickbite.storage.s3.*} properties.
 */
@ConfigurationProperties(prefix = "quickbite.storage.s3")
public record StorageProperties(
        String bucketName,
        String region,
        Duration presignedUrlExpiry
) {
    public StorageProperties {
        if (presignedUrlExpiry == null) {
            presignedUrlExpiry = Duration.ofMinutes(10);
        }
    }
}
