package com.quickbite.quickbite.onboarding.service.deliveryagent;

import com.quickbite.quickbite.delivery.model.DeliveryAgentDocumentType;
import com.quickbite.quickbite.onboarding.dto.deliveryagent.DeliveryAgentApplicationDocumentRequest;
import com.quickbite.quickbite.onboarding.dto.deliveryagent.DeliveryAgentApplicationDocumentResponse;
import com.quickbite.quickbite.onboarding.dto.deliveryagent.DeliveryAgentApplicationResponse;
import com.quickbite.quickbite.onboarding.dto.vehicle.VehicleApplicationResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing delivery agent onboarding applications.
 */
public interface DeliveryAgentApplicationService {

    DeliveryAgentApplicationResponse startApplication(UUID agentUserId);

    DeliveryAgentApplicationResponse getCurrentApplication(UUID agentUserId);

    DeliveryAgentApplicationResponse getApplication(UUID id, UUID agentUserId);

    // Get all documents for a specific application
    List<DeliveryAgentApplicationDocumentResponse> getDocument(UUID id, UUID agentUserId);

    DeliveryAgentApplicationDocumentResponse addDocument(UUID id, UUID agentUserId, DeliveryAgentApplicationDocumentRequest request);

    void removeDocument(UUID id, UUID agentUserId, DeliveryAgentDocumentType type);

    VehicleApplicationResponse getCurrentVehicleApplication(UUID id, UUID agentUserId);

    VehicleApplicationResponse startVehicleApplication(UUID id, UUID agentUserId);

    DeliveryAgentApplicationResponse submitApplication(UUID id, UUID agentUserId);

    DeliveryAgentApplicationResponse reopenApplication(UUID id, UUID agentUserId);
}
