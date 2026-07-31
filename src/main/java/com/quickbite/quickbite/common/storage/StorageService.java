package com.quickbite.quickbite.common.storage;

import java.time.Duration;

/**
 * Port for presigned-URL-based file storage. Implementations may back this with
 * AWS S3, GCS, Azure Blob, or a local stub for tests.
 */
public interface StorageService {
    PresignedUploadResult generateUploadUrl(String key, String contentType, Duration expiry);
}
