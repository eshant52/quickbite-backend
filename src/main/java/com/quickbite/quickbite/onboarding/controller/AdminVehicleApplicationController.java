package com.quickbite.quickbite.onboarding.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.onboarding.dto.AdminRejectRequest;
import com.quickbite.quickbite.onboarding.dto.vehicle.VehicleApplicationResponse;
import com.quickbite.quickbite.onboarding.dto.vehicle.VehicleApplicationSummaryResponse;
import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.service.vehicle.AdminVehicleApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/vehicle-applications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVehicleApplicationController {

    private final AdminVehicleApplicationService adminVehicleApplicationService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public AdminVehicleApplicationController(
            AdminVehicleApplicationService adminVehicleApplicationService,
            AuthenticatedSessionResolver authenticatedSessionResolver
    ) {
        this.adminVehicleApplicationService = adminVehicleApplicationService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    @GetMapping
    public ResponseEntity<CursorPage<VehicleApplicationSummaryResponse>> listVehicleApplications(
            @RequestParam(value = "status", required = false) ApplicationStatus status,
            @RequestParam(value = "cursor", required = false) UUID cursor,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminVehicleApplicationService.listVehicleApplications(status, cursor, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleApplicationResponse> getVehicleApplication(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(adminVehicleApplicationService.getVehicleApplicationAsAdmin(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveVehicleApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID adminId = authenticatedSessionResolver.userIdFromJwt(jwt);
        adminVehicleApplicationService.approveVehicleApplication(id, adminId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectVehicleApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody @Valid AdminRejectRequest request
    ) {
        UUID adminId = authenticatedSessionResolver.userIdFromJwt(jwt);
        adminVehicleApplicationService.rejectVehicleApplication(id, adminId, request.remarks());
        return ResponseEntity.ok().build();
    }
}
