package com.quickbite.quickbite.vehicle.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.vehicle.dto.ExpireVehicleOwnershipRequest;
import com.quickbite.quickbite.vehicle.dto.VehicleDetailResponse;
import com.quickbite.quickbite.vehicle.dto.VehicleSummaryResponse;
import com.quickbite.quickbite.vehicle.service.AdminVehicleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/vehicles")
public class AdminVehicleController {

    private final AdminVehicleService adminVehicleService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public AdminVehicleController(
            AdminVehicleService adminVehicleService,
            AuthenticatedSessionResolver authenticatedSessionResolver
    ) {
        this.adminVehicleService = adminVehicleService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    @GetMapping("/agents/{agentId}")
    public ResponseEntity<List<VehicleSummaryResponse>> getVehiclesForAgent(
            @PathVariable UUID agentId
    ) {
        return ResponseEntity.ok(adminVehicleService.getVehiclesForAgent(agentId));
    }

    @GetMapping("/ownerships/{ownershipId}")
    public ResponseEntity<VehicleDetailResponse> getVehicleOwnershipDetail(
            @PathVariable UUID ownershipId
    ) {
        return ResponseEntity.ok(adminVehicleService.getVehicleOwnershipDetail(ownershipId));
    }

    @PatchMapping("/ownerships/{ownershipId}/expire")
    public ResponseEntity<VehicleDetailResponse> expireVehicleOwnership(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID ownershipId,
            @RequestBody @Valid ExpireVehicleOwnershipRequest request
    ) {
        UUID adminUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(adminVehicleService.expireVehicleOwnership(ownershipId, adminUserId, request.remarks()));
    }
}
