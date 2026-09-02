package com.quickbite.quickbite.onboarding.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.onboarding.dto.vehicle.*;
import com.quickbite.quickbite.onboarding.service.vehicle.VehicleApplicationService;
import com.quickbite.quickbite.vehicle.model.VehicleOwnershipDocumentType;
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
 * Shared Vehicle Application controller.
 *
 * Used by two flows:
 *  - Flow 1 (Onboarding):  vehicleAppId is obtained from DeliveryAgentApplicationController
 *                           (POST /{id}/vehicle/applications/start), then details/docs filled here.
 *  - Flow 2 (Standalone):  Approved agents POST /start to create a new standalone vehicle application,
 *                           then fill details/docs using the same endpoints.
 *
 * Extra:  GET /check-vin  — pre-check whether a VIN already exists in the system.
 *
 * Step 0: List / get / start.
 * Step 1: Vehicle details  (GET + PATCH /{id}/details).
 * Step 2: Vehicle documents (GET + POST + DELETE /{id}/documents/{type}).
 * Step 3: Submit / reopen  (POST /{id}/submit, POST /{id}/reopen).
 */
@RestController
@RequestMapping("/api/v1/onboarding/vehicle-applications")
@PreAuthorize("hasRole('DELIVERY_AGENT')")
public class VehicleApplicationController {

    private final VehicleApplicationService vehicleApplicationService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public VehicleApplicationController(
            VehicleApplicationService vehicleApplicationService,
            AuthenticatedSessionResolver authenticatedSessionResolver
    ) {
        this.vehicleApplicationService = vehicleApplicationService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    // ─── Extra: VIN check ────────────────────────────────────────────────────

    @GetMapping("/check-vin")
    public ResponseEntity<CheckVinResponse> checkVin(
            @RequestParam String vin
    ) {
        return ResponseEntity.ok(vehicleApplicationService.checkVin(vin));
    }

    // ─── Step 0: List / get / start ──────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<VehicleApplicationResponse>> getMyVehicleApplications(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(vehicleApplicationService.getMyVehicleApplications(agentUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleApplicationResponse> getVehicleApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(vehicleApplicationService.getVehicleApplication(id, agentUserId));
    }

    /** Standalone start — only for already-approved agents adding a second+ vehicle. */
    @PostMapping("/start")
    public ResponseEntity<VehicleApplicationResponse> startApplication(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleApplicationService.startApplication(agentUserId));
    }

    // ─── Step 1: Vehicle details ──────────────────────────────────────────────

    @GetMapping("/{id}/details")
    public ResponseEntity<VehicleApplicationDetailsResponse> getVehicleDetails(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(vehicleApplicationService.getVehicleDetails(id, agentUserId));
    }

    @PatchMapping("/{id}/details")
    public ResponseEntity<VehicleApplicationDetailsResponse> saveVehicleDetails(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody @Valid DeliveryAgentApplicationVehicleRequest request
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(vehicleApplicationService.saveVehicleDetails(id, agentUserId, request));
    }

    // ─── Step 2: Vehicle documents ────────────────────────────────────────────

    @GetMapping("/{id}/documents")
    public ResponseEntity<List<VehicleApplicationDocumentResponse>> getVehicleDocuments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(vehicleApplicationService.getVehicleDocuments(id, agentUserId));
    }

    @PostMapping("/{id}/documents")
    public ResponseEntity<VehicleApplicationDocumentResponse> saveVehicleDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody @Valid DeliveryAgentApplicationVehicleDocumentRequest request
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleApplicationService.saveVehicleDocument(id, agentUserId, request));
    }

    @DeleteMapping("/{id}/documents/{type}")
    public ResponseEntity<Void> removeVehicleDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable VehicleOwnershipDocumentType type
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        vehicleApplicationService.removeVehicleDocument(id, agentUserId, type);
        return ResponseEntity.noContent().build();
    }

    // ─── Step 3: Submit / reopen ──────────────────────────────────────────────

    @PostMapping("/{id}/submit")
    public ResponseEntity<VehicleApplicationResponse> submitVehicleApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(vehicleApplicationService.submitVehicleApplication(id, agentUserId));
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<VehicleApplicationResponse> reopenVehicleApplication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(vehicleApplicationService.reopenVehicleApplication(id, agentUserId));
    }
}
