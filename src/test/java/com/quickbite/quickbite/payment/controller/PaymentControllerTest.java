package com.quickbite.quickbite.payment.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.payment.dto.PaymentResponse;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;
import com.quickbite.quickbite.payment.service.PaymentQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentQueryService paymentQueryService;

    @Mock
    private AuthenticatedSessionResolver authenticatedSessionResolver;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    @DisplayName("getPaymentByOrder resolves customer ID, calls PaymentQueryService, and returns 200 OK")
    void getPaymentByOrder_success() {
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentResponse sampleResponse = new PaymentResponse(
                UUID.randomUUID(),
                orderId,
                PaymentMethod.UPI,
                new BigDecimal("25.50"),
                PaymentStatus.SUCCESS,
                Instant.now()
        );

        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(customerId);
        when(paymentQueryService.getPaymentByOrderId(orderId, customerId)).thenReturn(sampleResponse);

        ResponseEntity<PaymentResponse> response = paymentController.getPaymentByOrder(jwt, orderId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(sampleResponse);
        verify(paymentQueryService).getPaymentByOrderId(orderId, customerId);
    }
}
