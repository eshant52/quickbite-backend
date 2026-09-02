package com.quickbite.quickbite.onboarding.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.delivery.model.DeliveryAgentDocumentType;
import com.quickbite.quickbite.onboarding.dto.deliveryagent.DeliveryAgentApplicationDocumentRequest;
import com.quickbite.quickbite.onboarding.dto.deliveryagent.DeliveryAgentApplicationDocumentResponse;
import com.quickbite.quickbite.onboarding.dto.deliveryagent.DeliveryAgentApplicationResponse;
import com.quickbite.quickbite.onboarding.dto.vehicle.VehicleApplicationResponse;
import com.quickbite.quickbite.onboarding.service.deliveryagent.DeliveryAgentApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Delivery Agent Onboarding controller.
 *
 * Step 0: Create / get the application.
 * Step 1: Personal identity documents (AADHAR, DRIVING_LICENSE).
 * Step 2: Start the primary vehicle application (details & docs handled by VehicleApplicationController).
 * Step 3: Submit / reopen.
 */
@RestController
@RequestMapping("/api/v1/onboarding/delivery-agent")
@PreAuthorize("hasRole('DELIVERY_AGENT')")
public class DeliveryAgentApplicationController {

    private final DeliveryAgentApplicationService applicationService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public DeliveryAgentApplicationController(
            DeliveryAgentApplicationService applicationService,
            AuthenticatedSessionResolver authenticatedSessionResolver
    ) {
        this.applicationService = applicationService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    // ─── Step 0: Create / get ────────────────────────────────────────────────

    @PostMapping("/start")
    public ResponseEntity<DeliveryAgentApplicationResponse> startApplication(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.startApplication(agentUserId));
    }

    @GetMapping("/current")
    public ResponseEntity<DeliveryAgentApplicationResponse> getCurrentApplication(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.getCurrentApplication(agentUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryAgentApplicationResponse> getApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.getApplication(id, agentUserId));
    }

    // ─── Step 1: Personal identity documents ─────────────────────────────────

    @GetMapping("/{id}/documents")
    public ResponseEntity<List<DeliveryAgentApplicationDocumentResponse>> getDocuments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.getDocument(id, agentUserId));
    }

    @PostMapping("/{id}/documents")
    public ResponseEntity<DeliveryAgentApplicationDocumentResponse> addDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody @Valid DeliveryAgentApplicationDocumentRequest request
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.addDocument(id, agentUserId, request));
    }

    @DeleteMapping("/{id}/documents/{type}")
    public ResponseEntity<Void> removeDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable DeliveryAgentDocumentType type
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        applicationService.removeDocument(id, agentUserId, type);
        return ResponseEntity.noContent().build();
    }

    // ─── Step 2: Vehicle application (start / get) ───────────────────────────
    // Details and documents for the vehicle are handled by VehicleApplicationController
    // using the vehicleAppId returned by these endpoints.

    @PostMapping("/{id}/vehicle/applications/start")
    public ResponseEntity<VehicleApplicationResponse> startVehicleApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.startVehicleApplication(id, agentUserId));
    }

    @GetMapping("/{id}/vehicle/applications/current")
    public ResponseEntity<VehicleApplicationResponse> getCurrentVehicleApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.getCurrentVehicleApplication(id, agentUserId));
    }

    // ─── Step 3: Submit / reopen ─────────────────────────────────────────────

    @PostMapping("/{id}/submit")
    public ResponseEntity<DeliveryAgentApplicationResponse> submitApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.submitApplication(id, agentUserId));
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<DeliveryAgentApplicationResponse> reopenApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.reopenApplication(id, agentUserId));
    }
}
