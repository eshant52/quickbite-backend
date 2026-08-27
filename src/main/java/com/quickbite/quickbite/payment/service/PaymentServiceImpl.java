package com.quickbite.quickbite.payment.service;

import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.payment.dto.PaymentResponse;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.exception.PaymentNotFoundException;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.repository.PaymentRepository;
import com.quickbite.quickbite.payment.repository.PaymentStatusHistoryRepository;
import com.quickbite.quickbite.payment.service.strategy.PaymentStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentStatusHistoryRepository paymentStatusHistoryRepository;
    private final OrderRepository orderRepository;
    private final List<PaymentStrategy> strategies;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            PaymentStatusHistoryRepository paymentStatusHistoryRepository,
            OrderRepository orderRepository,
            List<PaymentStrategy> strategies) {
        this.paymentRepository = paymentRepository;
        this.paymentStatusHistoryRepository = paymentStatusHistoryRepository;
        this.orderRepository = orderRepository;
        this.strategies = strategies;
    }

    @Override
    @Transactional
    public PaymentResult initiatePayment(Order order, PaymentMethod method) {
        PaymentStrategy strategy = strategies.stream()
                .filter(s -> s.supports(method))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unsupported payment method: " + method));

        return strategy.initiate(order, method);
    }

    @Override
    public PaymentResponse getPaymentByOrderId(UUID orderId, UUID customerId) {
        // Verify that the order belongs to the customer
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order " + orderId));

        return new PaymentResponse(
                payment.getId(),
                order.getId(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getCurrentStatus(),
                payment.getCreatedAt()
        );
    }
}
