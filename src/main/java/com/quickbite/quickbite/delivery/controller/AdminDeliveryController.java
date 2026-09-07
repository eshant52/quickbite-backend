package com.quickbite.quickbite.delivery.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.delivery.dto.AdminSuspendDeliveryAgentRequest;
import com.quickbite.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.quickbite.delivery.model.DeliveryAgentVerificationStatus;
import com.quickbite.quickbite.delivery.service.AdminDeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/delivery-agents")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDeliveryController {

    private final AdminDeliveryService adminDeliveryService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public AdminDeliveryController(
            AdminDeliveryService adminDeliveryService,
            AuthenticatedSessionResolver authenticatedSessionResolver
    ) {
        this.adminDeliveryService = adminDeliveryService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    @GetMapping
    public ResponseEntity<CursorPage<DeliveryAgentResponse>> listAgents(
            @RequestParam(value = "status", required = false) DeliveryAgentVerificationStatus status,
            @RequestParam(value = "cursor", required = false) UUID cursor,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminDeliveryService.listAgentsByStatus(status, cursor, size));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<DeliveryAgentResponse> suspendAgent(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody @Valid AdminSuspendDeliveryAgentRequest request
    ) {
        UUID adminId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(adminDeliveryService.suspendAgent(id, adminId, request.reason()));
    }

    @PostMapping("/{id}/reinstate")
    public ResponseEntity<DeliveryAgentResponse> reinstateAgent(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID adminId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(adminDeliveryService.reinstateAgent(id, adminId));
    }
}
