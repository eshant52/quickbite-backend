package com.quickbite.quickbite.delivery.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.quickbite.delivery.model.DeliveryAgentVerificationStatus;

import java.util.UUID;

public interface AdminDeliveryService {

    DeliveryAgentResponse suspendAgent(UUID agentId, UUID adminId, String reason);

    DeliveryAgentResponse reinstateAgent(UUID agentId, UUID adminId);

    CursorPage<DeliveryAgentResponse> listAgentsByStatus(DeliveryAgentVerificationStatus status, UUID cursor, int size);
}
