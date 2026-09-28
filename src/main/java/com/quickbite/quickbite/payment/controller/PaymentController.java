package com.quickbite.quickbite.payment.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.payment.dto.OnlinePaymentVerifyRequest;
import com.quickbite.quickbite.payment.dto.PaymentResponse;
import com.quickbite.quickbite.payment.service.PaymentProcessingService;
import com.quickbite.quickbite.payment.service.PaymentQueryService;
import jakarta.validation.Valid;
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

    private final PaymentQueryService paymentQueryService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;
    private final PaymentProcessingService paymentProcessingService;

    public PaymentController(
            PaymentQueryService paymentQueryService,
            AuthenticatedSessionResolver authenticatedSessionResolver, PaymentProcessingService paymentProcessingService) {
        this.paymentQueryService = paymentQueryService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
        this.paymentProcessingService = paymentProcessingService;
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
        return ResponseEntity.ok(paymentQueryService.getPaymentByOrderId(orderId, customerId));
    }

    /**
     * Verify the HMAC-SHA256 signature returned by the gateway's Checkout UI to the client,
     * then transitions payment → SUCCESS and order → PLACED atomically.
     *
     * @param request The verification request containing the gateway order ID, payment ID, and signature.
     * @return A response entity indicating the success of the verification.
     */
    @PostMapping("/verify")
    public ResponseEntity<Void> verifyOnlinePayment(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid OnlinePaymentVerifyRequest request) {
        UUID customerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        paymentProcessingService.verifyOnlinePayment(
                customerId,
                request.gatewayOrderId(),
                request.gatewayPaymentId(),
                request.gatewaySignature()
        );
        return ResponseEntity.ok().build();
    }
}
