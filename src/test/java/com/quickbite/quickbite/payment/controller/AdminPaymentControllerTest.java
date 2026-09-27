package com.quickbite.quickbite.payment.controller;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPaymentControllerTest {

    @Mock
    private PaymentQueryService paymentQueryService;

    @InjectMocks
    private AdminPaymentController adminPaymentController;

    @Test
    @DisplayName("GET /api/v1/admin/payments/order/{orderId} returns PaymentResponse for any order")
    void getPaymentByOrderReturnsPaymentResponse() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentResponse expected = new PaymentResponse(
                paymentId,
                orderId,
                "TXN-100",
                PaymentMethod.UPI,
                new BigDecimal("499.00"),
                PaymentStatus.REFUND_FAILED,
                "order_rzp_1",
                "pay_rzp_1",
                Instant.now(),
                List.of()
        );
        when(paymentQueryService.getPaymentByOrderIdForAdmin(orderId)).thenReturn(expected);

        ResponseEntity<PaymentResponse> response = adminPaymentController.getPaymentByOrder(orderId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(paymentQueryService).getPaymentByOrderIdForAdmin(orderId);
    }
}
