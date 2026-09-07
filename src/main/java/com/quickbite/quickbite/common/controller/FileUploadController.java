package com.quickbite.quickbite.common.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.common.dto.FileUploadUrlRequest;
import com.quickbite.quickbite.common.dto.FileUploadUrlResponse;
import com.quickbite.quickbite.common.dto.UploadContext;
import com.quickbite.quickbite.common.config.property.StorageProperties;
import com.quickbite.quickbite.common.storage.PresignedUploadResult;
import com.quickbite.quickbite.common.storage.StorageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Issues short-lived S3 presigned PUT URLs so clients can upload files directly to S3
 * without routing bytes through this server.
 *
 * Flow:
 *   1. Client → POST /api/v1/files/upload-url  { context, applicationId, contentType, fileName }
 *   2. Server → { uploadUrl, fileUrl }
 *   3. Client → PUT {uploadUrl} with file bytes (direct to S3, no auth header)
 *   4. Client → POST /api/v1/restaurant/applications/{id}/images  { imageUrl: fileUrl }
 */
@RestController
@RequestMapping("/api/v1/files")
public class FileUploadController {

    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final AuthenticatedSessionResolver sessionResolver;

    public FileUploadController(StorageService storageService,
                                 StorageProperties storageProperties,
                                 AuthenticatedSessionResolver sessionResolver) {
        this.storageService = storageService;
        this.storageProperties = storageProperties;
        this.sessionResolver = sessionResolver;
    }

    @PostMapping("/upload-url")
    public ResponseEntity<FileUploadUrlResponse> getUploadUrl(@Valid @RequestBody FileUploadUrlRequest request,
                                                               @AuthenticationPrincipal Jwt jwt) {
        String extension = extractExtension(request.fileName());
        String key = buildS3Key(request.context(), request.applicationId(), extension);

        PresignedUploadResult result = storageService.generateUploadUrl(
                key, request.contentType(), storageProperties.presignedUrlExpiry());

        return ResponseEntity.ok(new FileUploadUrlResponse(result.uploadUrl(), result.fileUrl()));
    }

    /**
     * Builds the S3 object key based on context and application ID.
     * <ul>
     *   <li>RESTAURANT_IMAGE    → restaurant-applications/{appId}/images/{uuid}.{ext}</li>
     *   <li>RESTAURANT_DOCUMENT → restaurant-applications/{appId}/documents/{uuid}.{ext}</li>
     * </ul>
     */
    private String buildS3Key(UploadContext context, UUID applicationId, String extension) {
        String folder = switch (context) {
            case RESTAURANT_IMAGE    -> "images";
            case RESTAURANT_DOCUMENT -> "documents";
        };
        return String.format("restaurant-applications/%s/%s/%s.%s",
                applicationId, folder, UUID.randomUUID(), extension);
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "bin";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
