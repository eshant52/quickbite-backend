package com.quickbite.quickbite.payment.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.payment.dto.PaymentResponse;
import com.quickbite.quickbite.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/payments")
@PreAuthorize("hasRole('CUSTOMER')")
public class PaymentController {

    private final PaymentService paymentService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public PaymentController(
            PaymentService paymentService,
            AuthenticatedSessionResolver authenticatedSessionResolver) {
        this.paymentService = paymentService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    /**
     * Fetch the payment status for a given order.
     * The customer uses this to poll payment status after returning from a gateway redirect.
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId) {
        UUID customerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId, customerId));
    }
}
