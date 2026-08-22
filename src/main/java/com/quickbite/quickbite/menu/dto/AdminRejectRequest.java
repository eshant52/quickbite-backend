package com.quickbite.quickbite.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminRejectRequest(
        @NotBlank(message = "Rejection remarks are required")
        @Size(max = 500, message = "Remarks must be at most 500 characters") String remarks
) {
}
