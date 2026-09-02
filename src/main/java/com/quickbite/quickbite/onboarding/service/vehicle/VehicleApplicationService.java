package com.quickbite.quickbite.onboarding.service.vehicle;

import com.quickbite.quickbite.onboarding.dto.vehicle.*;
import com.quickbite.quickbite.vehicle.model.VehicleOwnershipDocumentType;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing standalone vehicle applications submitted by approved delivery agents.
 */
public interface VehicleApplicationService {

    CheckVinResponse checkVin(String vinNumber);

    List<VehicleApplicationResponse> getMyVehicleApplications(UUID agentUserId);

    VehicleApplicationResponse getVehicleApplication(UUID vehicleAppId, UUID agentUserId);

    VehicleApplicationResponse startApplication(UUID agentUserId);

    VehicleApplicationDetailsResponse saveVehicleDetails(UUID id, UUID agentUserId, DeliveryAgentApplicationVehicleRequest request);

    VehicleApplicationDetailsResponse getVehicleDetails(UUID vehicleAppId, UUID agentUserId);

    VehicleApplicationDocumentResponse saveVehicleDocument(UUID vehicleAppId, UUID agentUserId, DeliveryAgentApplicationVehicleDocumentRequest request);

    List<VehicleApplicationDocumentResponse> getVehicleDocuments(UUID vehicleAppId, UUID agentUserId);

    void removeVehicleDocument(UUID vehicleAppId, UUID agentUserId, VehicleOwnershipDocumentType type);

    VehicleApplicationResponse submitVehicleApplication(UUID vehicleAppId, UUID agentUserId);

    VehicleApplicationResponse reopenVehicleApplication(UUID vehicleAppId, UUID agentUserId);
}
