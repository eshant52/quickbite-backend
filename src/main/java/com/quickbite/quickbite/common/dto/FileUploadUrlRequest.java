package com.quickbite.quickbite.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FileUploadUrlRequest(
        @NotNull UploadContext context,
        @NotNull UUID applicationId,
        @NotBlank String contentType,
        @NotBlank String fileName
) {}
