package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.order.exception.OrderNotFoundException;
import com.quickbite.quickbite.order.exception.OrderStateException;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.payment.service.PaymentProcessingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final PaymentProcessingService paymentService;

    public AdminOrderServiceImpl(
            OrderRepository orderRepository,
            PaymentProcessingService paymentService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
    }

    @Override
    @Transactional
    public void refundDeliveredOrder(UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (order.getCurrentStatus() != OrderStatus.DELIVERED) {
            throw new OrderStateException("Only DELIVERED orders are eligible for a post-delivery refund");
        }

        paymentService.refundSuccessfulPayment(order.getId(), reason);
    }
}
