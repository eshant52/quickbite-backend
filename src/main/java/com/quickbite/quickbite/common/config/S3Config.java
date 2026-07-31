package com.quickbite.quickbite.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Creates the AWS S3 client beans. Credentials are resolved via the default
 * AWS credential chain (environment variables, instance profile, ~/.aws/credentials).
 */
@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(StorageProperties properties) {
        return S3Client.builder()
                .region(Region.of(properties.region()))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(StorageProperties properties) {
        return S3Presigner.builder()
                .region(Region.of(properties.region()))
                .build();
    }
}
