package com.quickbite.quickbite.vehicle.service;

import com.quickbite.quickbite.vehicle.dto.VehicleDetailResponse;
import com.quickbite.quickbite.vehicle.dto.VehicleSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface AdminVehicleService {

    List<VehicleSummaryResponse> getVehiclesForAgent(UUID deliveryAgentId);

    VehicleDetailResponse getVehicleOwnershipDetail(UUID ownershipId);

    VehicleDetailResponse expireVehicleOwnership(UUID ownershipId, UUID adminUserId, String remarks);
}
