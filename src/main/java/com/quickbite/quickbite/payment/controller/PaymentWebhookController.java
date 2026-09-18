package com.quickbite.quickbite.payment.controller;

import com.quickbite.quickbite.payment.dto.WebhookPayloadRequest;
import com.quickbite.quickbite.payment.service.PaymentWebhookService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks/payment")
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    public PaymentWebhookController(PaymentWebhookService paymentWebhookService) {
        this.paymentWebhookService = paymentWebhookService;
    }

    /**
     * Local development stub webhook — simulates a gateway callback without real credentials.
     * Only active in the {@code dev} Spring profile via {@link com.quickbite.quickbite.payment.service.strategy.StubOnlinePaymentStrategy}.
     */
    @PostMapping("/stub")
    public ResponseEntity<Void> handleStubWebhook(
            @RequestBody @Valid WebhookPayloadRequest payloadRequest) {
        paymentWebhookService.handleStubOnlineWebhook(payloadRequest.transactionId(), payloadRequest.status());
        return ResponseEntity.ok().build();
    }

    /**
     * Server-to-server Razorpay webhook endpoint.
     *
     * <p>Razorpay calls this endpoint asynchronously when a payment event occurs
     * (e.g. {@code payment.captured}, {@code payment.failed}).
     * This endpoint is public (no JWT required) — Razorpay authenticates via
     * the {@code X-Razorpay-Signature} HMAC-SHA256 header.
     *
     * <p><b>IMPORTANT:</b> The raw request body must be read as a {@code String}
     * and passed verbatim to the service. Do not use {@code @RequestBody POJO} here
     * as re-serialization changes whitespace and breaks the HMAC verification.
     */
    @PostMapping(value = "/razorpay", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> handleRazorpayWebhook(
            @RequestBody String rawBody,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        paymentWebhookService.handleRazorpayWebhook(rawBody, signature);
        return ResponseEntity.ok().build();
    }
}
