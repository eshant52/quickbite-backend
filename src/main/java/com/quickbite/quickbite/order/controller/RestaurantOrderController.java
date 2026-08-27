package com.quickbite.quickbite.order.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.order.dto.OrderResponse;
import com.quickbite.quickbite.order.dto.OrderSummaryResponse;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.service.RestaurantOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurant/orders")
@PreAuthorize("hasRole('RESTAURANT_OWNER')")
public class RestaurantOrderController {

    private final RestaurantOrderService restaurantOrderService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public RestaurantOrderController(
            RestaurantOrderService restaurantOrderService,
            AuthenticatedSessionResolver authenticatedSessionResolver) {
        this.restaurantOrderService = restaurantOrderService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }


    @GetMapping
    public ResponseEntity<CursorPage<OrderSummaryResponse>> listRestaurantOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("restaurantId")  UUID restaurantId,
            @RequestParam(value = "status", required = false) OrderStatus status,
            @RequestParam(value = "cursor", required = false) UUID cursor,
            @RequestParam(value = "size", defaultValue = "20") int size
            ) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantOrderService.listRestaurantOrders(restaurantId, ownerId, status, cursor, size));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getRestaurantOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId,
            @RequestParam("restaurantId") UUID restaurantId) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantOrderService.getRestaurantOrder(orderId, restaurantId, ownerId));
    }

    @PostMapping("/{orderId}/accept")
    public ResponseEntity<OrderResponse> acceptRestaurantOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId,
            @RequestParam("restaurantId") UUID restaurantId) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantOrderService.acceptOrder(orderId, restaurantId, ownerId));
    }

    @PostMapping("/{orderId}/decline")
    public ResponseEntity<OrderResponse> declineRestaurantOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId,
            @RequestParam("restaurantId") UUID restaurantId) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantOrderService.declineOrder(orderId, restaurantId, ownerId));
    }

    @PostMapping("/{orderId}/mark-preparing")
    public ResponseEntity<OrderResponse> markPreparingRestaurantOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId,
            @RequestParam("restaurantId") UUID restaurantId) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantOrderService.markPreparing(orderId, restaurantId, ownerId));
    }

    @PostMapping("/{orderId}/mark-ready")
    public ResponseEntity<OrderResponse> markReadyForPickupRestaurantOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId,
            @RequestParam("restaurantId") UUID restaurantId) {
        UUID ownerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(restaurantOrderService.markReadyForPickup(orderId, restaurantId, ownerId));
    }
}
