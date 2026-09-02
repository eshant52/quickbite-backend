package com.quickbite.quickbite.onboarding.service.vehicle;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.onboarding.dto.vehicle.VehicleApplicationResponse;
import com.quickbite.quickbite.onboarding.dto.vehicle.VehicleApplicationSummaryResponse;
import com.quickbite.quickbite.onboarding.model.ApplicationStatus;

import java.util.UUID;

/**
 * Service interface for managing standalone vehicle applications from an admin perspective.
 */
public interface AdminVehicleApplicationService {

    CursorPage<VehicleApplicationSummaryResponse> listVehicleApplications(ApplicationStatus status, UUID cursor, int size);

    VehicleApplicationResponse getVehicleApplicationAsAdmin(UUID vehicleAppId);

    void approveVehicleApplication(UUID vehicleAppId, UUID adminId);

    void rejectVehicleApplication(UUID vehicleAppId, UUID adminId, String remarks);
}
