package com.quickbite.quickbite.onboarding.dto.deliveryagent;

import com.quickbite.quickbite.delivery.model.DeliveryAgentDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeliveryAgentApplicationDocumentRequest(
        @NotNull(message = "Document type is required")
        DeliveryAgentDocumentType type,

        @NotBlank(message = "Document URL is required")
        String url
) {}
