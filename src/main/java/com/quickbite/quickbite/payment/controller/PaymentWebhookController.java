package com.quickbite.quickbite.payment.controller;

import com.quickbite.quickbite.payment.dto.WebhookPayloadRequest;
import com.quickbite.quickbite.payment.service.PaymentWebhookService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/payment")
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    public PaymentWebhookController(PaymentWebhookService paymentWebhookService) {
        this.paymentWebhookService = paymentWebhookService;
    }

    @PostMapping("/stub")
    public ResponseEntity<Void> handleStubWebhook(
            @RequestBody @Valid WebhookPayloadRequest payloadRequest
            ) {
        paymentWebhookService.handleWebhook(payloadRequest.transactionId(), payloadRequest.status());
        return ResponseEntity.ok().build();
    }
}
