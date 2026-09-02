package com.quickbite.quickbite.delivery.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.quickbite.delivery.dto.UpdateLocationRequest;
import com.quickbite.quickbite.delivery.service.DeliveryService;
import com.quickbite.quickbite.order.dto.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@PreAuthorize("hasRole('DELIVERY_AGENT')")
@RequestMapping("/api/v1/delivery-agent")
public class DeliveryAgentController {

    private final DeliveryService deliveryService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public DeliveryAgentController(
            DeliveryService deliveryService,
            AuthenticatedSessionResolver authenticatedSessionResolver
    ) {
        this.deliveryService = deliveryService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    @GetMapping("/profile")
    public ResponseEntity<DeliveryAgentResponse> getProfile(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(deliveryService.getMyProfile(userId));
    }

    @PutMapping("/location")
    public ResponseEntity<DeliveryAgentResponse> updateLocation(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UpdateLocationRequest request
    ) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(deliveryService.updateLocation(userId, request));
    }

    @PostMapping("/orders/{orderId}/pickup")
    public ResponseEntity<OrderResponse> markOutForDelivery(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(deliveryService.markOutForDelivery(orderId, agentUserId));
    }

    @PostMapping("/orders/{orderId}/delivered")
    public ResponseEntity<OrderResponse> markDelivered(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId
    ) {
        UUID agentUserId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(deliveryService.markDelivered(orderId, agentUserId));
    }
}
