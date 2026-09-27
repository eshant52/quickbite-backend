package com.quickbite.quickbite.order.controller;

import com.quickbite.quickbite.order.dto.AdminRefundOrderRequest;
import com.quickbite.quickbite.order.service.AdminOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    @Mock
    private AdminOrderService adminOrderService;

    @InjectMocks
    private AdminOrderController adminOrderController;

    @Test
    @DisplayName("POST /api/v1/admin/orders/{orderId}/refund delegates to AdminOrderService and returns 204 No Content")
    void refundDeliveredOrderDelegatesAndReturnsNoContent() {
        UUID orderId = UUID.randomUUID();
        AdminRefundOrderRequest request = new AdminRefundOrderRequest("Missing item reported by customer");

        ResponseEntity<Void> response = adminOrderController.refundDeliveredOrder(orderId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(adminOrderService).refundDeliveredOrder(orderId, "Missing item reported by customer");
    }
}
