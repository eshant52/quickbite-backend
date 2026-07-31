package com.quickbite.quickbite.common.storage;

/**
 * Carries the short-lived PUT presigned URL that the client uses to upload
 * directly to S3, plus the permanent public URL of the resulting object.
 */
public record PresignedUploadResult(String uploadUrl, String fileUrl) {}
