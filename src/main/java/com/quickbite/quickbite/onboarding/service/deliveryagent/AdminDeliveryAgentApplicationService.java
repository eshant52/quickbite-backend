package com.quickbite.quickbite.onboarding.service.deliveryagent;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.onboarding.dto.deliveryagent.DeliveryAgentApplicationResponse;
import com.quickbite.quickbite.onboarding.dto.deliveryagent.DeliveryAgentApplicationSummaryResponse;
import com.quickbite.quickbite.onboarding.model.ApplicationStatus;

import java.util.UUID;

/**
 * Service interface for managing delivery agent applications from an admin perspective.
 */
public interface AdminDeliveryAgentApplicationService {

    CursorPage<DeliveryAgentApplicationSummaryResponse> listApplications(ApplicationStatus status, UUID cursor, int size);

    DeliveryAgentApplicationResponse getApplicationAsAdmin(UUID appId);

    void approveApplication(UUID appId, UUID adminId);

    void rejectApplication(UUID appId, UUID adminId, String remarks);
}
