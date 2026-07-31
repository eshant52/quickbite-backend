package com.quickbite.quickbite.onboarding.service;

import com.quickbite.quickbite.onboarding.dto.*;
import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RestaurantApplicationService {

    // Owner operations
    ApplicationResponse startApplication(UUID ownerId);

    ApplicationResponse getCurrentApplication(UUID ownerId);

    ApplicationResponse getApplication(UUID appId, UUID ownerId);

    ApplicationResponse saveDetails(UUID appId, UUID ownerId, ApplicationDetailsRequest request);

    ApplicationResponse saveAddress(UUID appId, UUID ownerId, ApplicationAddressRequest request);

    ApplicationResponse saveHours(UUID appId, UUID ownerId, ApplicationHoursRequest request);

    ApplicationResponse addImage(UUID appId, UUID ownerId, ApplicationImageRequest request);

    void removeImage(UUID appId, UUID ownerId, UUID imageId);

    ApplicationResponse addDocument(UUID appId, UUID ownerId, ApplicationDocumentRequest request);

    void removeDocument(UUID appId, UUID ownerId, UUID documentId);

    ApplicationResponse submitApplication(UUID appId, UUID ownerId);

    ApplicationResponse reopenApplication(UUID appId, UUID ownerId);

    // Admin operations
    Page<ApplicationSummaryResponse> listApplications(ApplicationStatus status, Pageable pageable);

    ApplicationResponse getApplicationAsAdmin(UUID appId);

    void approveApplication(UUID appId, UUID adminId);

    void rejectApplication(UUID appId, UUID adminId, String remarks);
}
