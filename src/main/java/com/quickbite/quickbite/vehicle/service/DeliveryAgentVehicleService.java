package com.quickbite.quickbite.vehicle.service;

import com.quickbite.quickbite.vehicle.dto.VehicleDetailResponse;
import com.quickbite.quickbite.vehicle.dto.VehicleSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface DeliveryAgentVehicleService {

    List<VehicleSummaryResponse> getMyVehicles(UUID agentUserId);

    VehicleDetailResponse getMyVehicleDetail(UUID ownershipId, UUID agentUserId);

    VehicleSummaryResponse selectCurrentVehicle(UUID ownershipId, UUID agentUserId);

    VehicleSummaryResponse deactivateVehicle(UUID ownershipId, UUID agentUserId);
}
