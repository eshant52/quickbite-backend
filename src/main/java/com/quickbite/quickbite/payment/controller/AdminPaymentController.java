package com.quickbite.quickbite.payment.controller;

import com.quickbite.quickbite.payment.dto.PaymentResponse;
import com.quickbite.quickbite.payment.service.PaymentQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final PaymentQueryService paymentQueryService;

    public AdminPaymentController(PaymentQueryService paymentQueryService) {
        this.paymentQueryService = paymentQueryService;
    }

    /**
     * Fetch the payment status, all retry attempts, and status history (including REFUND_FAILED reasons)
     * for any order.
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentQueryService.getPaymentByOrderIdForAdmin(orderId));
    }
}
