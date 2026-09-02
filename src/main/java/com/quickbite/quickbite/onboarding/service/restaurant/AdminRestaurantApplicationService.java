package com.quickbite.quickbite.onboarding.service.restaurant;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.onboarding.dto.restaurant.RestaurantApplicationResponse;
import com.quickbite.quickbite.onboarding.dto.restaurant.RestaurantApplicationSummaryResponse;
import com.quickbite.quickbite.onboarding.model.ApplicationStatus;

import java.util.UUID;

/**
 * Service interface for managing restaurant applications from an admin perspective.
 */
public interface AdminRestaurantApplicationService {
    CursorPage<RestaurantApplicationSummaryResponse> listApplications(ApplicationStatus status, UUID cursor, int size);

    RestaurantApplicationResponse getApplicationAsAdmin(UUID appId);

    void approveApplication(UUID appId, UUID adminId);

    void rejectApplication(UUID appId, UUID adminId, String remarks);
}
