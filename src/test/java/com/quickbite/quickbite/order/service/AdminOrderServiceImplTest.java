package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.order.exception.OrderNotFoundException;
import com.quickbite.quickbite.order.exception.OrderStateException;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.payment.service.PaymentProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private PaymentProcessingService paymentService;

    private AdminOrderServiceImpl adminOrderService;
    private UUID orderId;
    private Order order;

    @BeforeEach
    void setUp() {
        adminOrderService = new AdminOrderServiceImpl(orderRepository, paymentService);
        orderId = UUID.randomUUID();
        order = new Order();
        order.setId(orderId);
    }

    @Test
    @DisplayName("refundDeliveredOrder triggers payment refund when order is DELIVERED")
    void refundDeliveredOrder_delivered_triggersPaymentRefund() {
        order.setCurrentStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        adminOrderService.refundDeliveredOrder(orderId, "Damaged items reported to support");

        verify(paymentService).refundSuccessfulPayment(orderId, "Damaged items reported to support");
    }

    @Test
    @DisplayName("refundDeliveredOrder throws OrderStateException when order is not DELIVERED")
    void refundDeliveredOrder_notDelivered_throwsOrderStateException() {
        order.setCurrentStatus(OrderStatus.PLACED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> adminOrderService.refundDeliveredOrder(orderId, "Premature refund"))
                .isInstanceOf(OrderStateException.class)
                .hasMessageContaining("Only DELIVERED orders");

        verify(paymentService, never()).refundSuccessfulPayment(any(), any());
    }

    @Test
    @DisplayName("refundDeliveredOrder throws OrderNotFoundException when order does not exist")
    void refundDeliveredOrder_notFound_throwsOrderNotFoundException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminOrderService.refundDeliveredOrder(orderId, "Refund reason"))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
