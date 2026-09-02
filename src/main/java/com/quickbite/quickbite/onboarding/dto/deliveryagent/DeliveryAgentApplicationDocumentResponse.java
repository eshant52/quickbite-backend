package com.quickbite.quickbite.onboarding.dto.deliveryagent;

import com.quickbite.quickbite.delivery.model.DeliveryAgentDocumentType;
import com.quickbite.quickbite.onboarding.model.deliveryagent.DeliveryAgentApplicationDocument;

import java.util.UUID;

public record DeliveryAgentApplicationDocumentResponse(
        UUID id,
        DeliveryAgentDocumentType type,
        String url
) {
    public static DeliveryAgentApplicationDocumentResponse from(DeliveryAgentApplicationDocument doc) {
        return new DeliveryAgentApplicationDocumentResponse(
                doc.getId(),
                doc.getType(),
                doc.getUrl()
        );
    }
}
