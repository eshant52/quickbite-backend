package com.quickbite.quickbite.vehicle.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.vehicle.dto.VehicleDetailResponse;
import com.quickbite.quickbite.vehicle.dto.VehicleSummaryResponse;
import com.quickbite.quickbite.vehicle.service.DeliveryAgentVehicleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('DELIVERY_AGENT')")
@RequestMapping("/api/v1/delivery-agent/vehicles")
public class DeliveryAgentVehicleController {

    private final DeliveryAgentVehicleService deliveryAgentVehicleService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public DeliveryAgentVehicleController(
            DeliveryAgentVehicleService deliveryAgentVehicleService,
            AuthenticatedSessionResolver authenticatedSessionResolver
    ) {
        this.deliveryAgentVehicleService = deliveryAgentVehicleService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    @GetMapping
    public ResponseEntity<List<VehicleSummaryResponse>> getMyVehicles(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(deliveryAgentVehicleService.getMyVehicles(userId));
    }

    @GetMapping("/{ownershipId}")
    public ResponseEntity<VehicleDetailResponse> getMyVehicleDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID ownershipId
    ) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(deliveryAgentVehicleService.getMyVehicleDetail(ownershipId, userId));
    }

    @PatchMapping("/{ownershipId}/select")
    public ResponseEntity<VehicleSummaryResponse> selectCurrentVehicle(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID ownershipId
    ) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(deliveryAgentVehicleService.selectCurrentVehicle(ownershipId, userId));
    }

    @PatchMapping("/{ownershipId}/deactivate")
    public ResponseEntity<VehicleSummaryResponse> deactivateVehicle(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID ownershipId
    ) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(deliveryAgentVehicleService.deactivateVehicle(ownershipId, userId));
    }
}
