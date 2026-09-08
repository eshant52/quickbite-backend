package com.quickbite.quickbite.payment.controller;

import com.quickbite.quickbite.payment.dto.WebhookPayloadRequest;
import com.quickbite.quickbite.payment.model.PaymentStatus;
import com.quickbite.quickbite.payment.service.PaymentWebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookControllerTest {

    @Mock
    private PaymentWebhookService paymentWebhookService;

    @InjectMocks
    private PaymentWebhookController paymentWebhookController;

    @Test
    @DisplayName("handleStubWebhook delegates to service and returns 200 OK")
    void handleStubWebhook_success() {
        WebhookPayloadRequest payload = new WebhookPayloadRequest("STUB-ABC12345", PaymentStatus.SUCCESS);

        ResponseEntity<Void> response = paymentWebhookController.handleStubWebhook(payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentWebhookService).handleWebhook("STUB-ABC12345", PaymentStatus.SUCCESS);
    }
}
