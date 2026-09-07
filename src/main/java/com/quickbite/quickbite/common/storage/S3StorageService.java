package com.quickbite.quickbite.common.storage;

import com.quickbite.quickbite.common.config.property.StorageProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

/**
 * AWS S3-backed {@link StorageService}. Generates a PUT presigned URL so the
 * client can upload directly to S3 without routing bytes through this server.
 */
@Component
public class S3StorageService implements StorageService {

    private final S3Presigner presigner;
    private final StorageProperties properties;

    public S3StorageService(S3Presigner presigner, StorageProperties properties) {
        this.presigner = presigner;
        this.properties = properties;
    }

    @Override
    public PresignedUploadResult generateUploadUrl(String key, String contentType, Duration expiry) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(properties.bucketName())
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

        String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                properties.bucketName(), properties.region(), key);

        return new PresignedUploadResult(presignedRequest.url().toString(), fileUrl);
    }
}
