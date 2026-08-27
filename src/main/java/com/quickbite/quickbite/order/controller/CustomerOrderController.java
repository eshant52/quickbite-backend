package com.quickbite.quickbite.order.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.order.dto.OrderResponse;
import com.quickbite.quickbite.order.dto.OrderSummaryResponse;
import com.quickbite.quickbite.order.dto.PlaceOrderRequest;
import com.quickbite.quickbite.order.service.CustomerOrderService;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public CustomerOrderController(
            CustomerOrderService customerOrderService,
            AuthenticatedSessionResolver authenticatedSessionResolver) {
        this.customerOrderService = customerOrderService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    /**
     * Place an order. Returns a PaymentResult whose {@code "type"} field tells the
     * client which SDK / flow to launch:
     * <ul>
     *   <li>{@code "COD"}         — no gateway action needed, order is already PLACED.</li>
     *   <li>{@code "STUB_ONLINE"} — redirect to {@code paymentUrl} (dev stub).</li>
     * </ul>
     */
    @PostMapping
    public ResponseEntity<PaymentResult> placeOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PlaceOrderRequest req) {
        UUID customerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        PaymentResult result = customerOrderService.placeOrder(customerId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    public ResponseEntity<CursorPage<OrderSummaryResponse>> listMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "cursor", required = false) UUID cursor,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        UUID customerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(customerOrderService.listMyOrders(customerId, cursor, size));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId) {
        UUID customerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(customerOrderService.getMyOrder(customerId, orderId));
    }

    @DeleteMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId) {
        UUID customerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        customerOrderService.cancelOrder(customerId, orderId);
        return ResponseEntity.noContent().build();
    }
}
