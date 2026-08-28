package com.quickbite.quickbite.payment.controller;

import com.quickbite.quickbite.payment.dto.WebhookPayloadRequest;
import com.quickbite.quickbite.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/payment")
public class PaymentWebhookController {

    private final PaymentService paymentService;

    public PaymentWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/stub")
    public ResponseEntity<Void> handleStubWebhook(
            @RequestBody @Valid WebhookPayloadRequest payloadRequest
            ) {
        paymentService.handleWebhook(payloadRequest.transactionId(), payloadRequest.status());
        return ResponseEntity.ok().build();
    }
}
