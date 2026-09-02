package com.quickbite.quickbite.onboarding.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.onboarding.dto.AdminRejectRequest;
import com.quickbite.quickbite.onboarding.dto.deliveryagent.DeliveryAgentApplicationResponse;
import com.quickbite.quickbite.onboarding.dto.deliveryagent.DeliveryAgentApplicationSummaryResponse;
import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.service.deliveryagent.AdminDeliveryAgentApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/delivery-agent-applications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDeliveryAgentApplicationController {

    private final AdminDeliveryAgentApplicationService adminApplicationService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public AdminDeliveryAgentApplicationController(
            AdminDeliveryAgentApplicationService adminApplicationService,
            AuthenticatedSessionResolver authenticatedSessionResolver
    ) {
        this.adminApplicationService = adminApplicationService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    @GetMapping
    public ResponseEntity<CursorPage<DeliveryAgentApplicationSummaryResponse>> listApplications(
            @RequestParam(value = "status", required = false) ApplicationStatus status,
            @RequestParam(value = "cursor", required = false) UUID cursor,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminApplicationService.listApplications(status, cursor, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryAgentApplicationResponse> getApplication(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(adminApplicationService.getApplicationAsAdmin(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID adminId = authenticatedSessionResolver.userIdFromJwt(jwt);
        adminApplicationService.approveApplication(id, adminId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody @Valid AdminRejectRequest request
    ) {
        UUID adminId = authenticatedSessionResolver.userIdFromJwt(jwt);
        adminApplicationService.rejectApplication(id, adminId, request.remarks());
        return ResponseEntity.ok().build();
    }
}
