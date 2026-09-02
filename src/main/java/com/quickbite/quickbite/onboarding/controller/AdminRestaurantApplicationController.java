package com.quickbite.quickbite.onboarding.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.onboarding.dto.AdminRejectRequest;
import com.quickbite.quickbite.onboarding.dto.restaurant.RestaurantApplicationResponse;
import com.quickbite.quickbite.onboarding.dto.restaurant.RestaurantApplicationSummaryResponse;
import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.service.restaurant.AdminRestaurantApplicationService;
import jakarta.validation.Valid;
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

    private final AdminRestaurantApplicationService applicationService;
    private final AuthenticatedSessionResolver sessionResolver;

    public AdminRestaurantApplicationController(AdminRestaurantApplicationService applicationService,
                                                AuthenticatedSessionResolver sessionResolver) {
        this.applicationService = applicationService;
        this.sessionResolver = sessionResolver;
    }

    /**
     * List applications filtered by status using cursor-based pagination.
     * <p>
     * Defaults to SUBMITTED (the queue admins normally work from).
     *
     * <pre>
     *   First page:  GET /api/v1/admin/restaurant/applications?status=SUBMITTED&size=20
     *   Next pages:  GET /api/v1/admin/restaurant/applications?status=SUBMITTED&cursor=&lt;nextCursor&gt;&size=20
     * </pre>
     *
     * Results are ordered by application ID (= creation time, since IDs are UUIDv7).
     */
    @GetMapping
    public ResponseEntity<CursorPage<RestaurantApplicationSummaryResponse>> listApplications(
            @RequestParam(defaultValue = "SUBMITTED") ApplicationStatus status,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(applicationService.listApplications(status, cursor, size));
    }

    /** Get the full detail of any application regardless of status. */
    @GetMapping("/{id}")
    public ResponseEntity<RestaurantApplicationResponse> getApplication(@PathVariable UUID id) {
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
