package com.quickbite.quickbite.onboarding.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.onboarding.dto.AdminRejectRequest;
import com.quickbite.quickbite.onboarding.dto.ApplicationResponse;
import com.quickbite.quickbite.onboarding.dto.ApplicationSummaryResponse;
import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.service.RestaurantApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin-facing REST controller for reviewing restaurant onboarding applications.
 * All endpoints require ROLE_ADMIN + SCOPE_API.
 */
@RestController
@RequestMapping("/api/v1/admin/restaurant/applications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRestaurantApplicationController {

    private final RestaurantApplicationService applicationService;
    private final AuthenticatedSessionResolver sessionResolver;

    public AdminRestaurantApplicationController(RestaurantApplicationService applicationService,
                                                AuthenticatedSessionResolver sessionResolver) {
        this.applicationService = applicationService;
        this.sessionResolver = sessionResolver;
    }

    /**
     * List applications filtered by status.
     * Defaults to SUBMITTED (the queue admins normally work from).
     * Supports Spring Data pagination: ?page=0&size=20&sort=updatedAt,desc
     */
    @GetMapping
    public ResponseEntity<Page<ApplicationSummaryResponse>> listApplications(
            @RequestParam(defaultValue = "SUBMITTED") ApplicationStatus status,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable) {
        return ResponseEntity.ok(applicationService.listApplications(status, pageable));
    }

    /** Get the full detail of any application regardless of status. */
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplication(@PathVariable UUID id) {
        return ResponseEntity.ok(applicationService.getApplicationAsAdmin(id));
    }

    /**
     * Approve an application (SUBMITTED or UNDER_REVIEW → APPROVED).
     * This atomically creates the Restaurant, Address, Hours, Images, Documents,
     * and StatusHistory rows in a single transaction.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveApplication(@PathVariable UUID id,
                                                    @AuthenticationPrincipal Jwt jwt) {
        UUID adminId = sessionResolver.userIdFromJwt(jwt);
        applicationService.approveApplication(id, adminId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reject an application with mandatory remarks (SUBMITTED or UNDER_REVIEW → REJECTED).
     * The owner can then reopen and resubmit.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectApplication(@PathVariable UUID id,
                                                   @Valid @RequestBody AdminRejectRequest request,
                                                   @AuthenticationPrincipal Jwt jwt) {
        UUID adminId = sessionResolver.userIdFromJwt(jwt);
        applicationService.rejectApplication(id, adminId, request.remarks());
        return ResponseEntity.noContent().build();
    }
}
