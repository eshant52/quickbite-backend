package com.quickbite.quickbite.payment.service;

import com.quickbite.quickbite.cart.repository.CartRepository;
import com.quickbite.quickbite.common.event.order.OrderPlacedEvent;
import com.quickbite.quickbite.common.event.payment.PaymentStatusChangedEvent;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.model.OrderStatusHistory;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.order.repository.OrderStatusHistoryRepository;
import com.quickbite.quickbite.payment.dto.PaymentResponse;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.exception.PaymentNotFoundException;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;
import com.quickbite.quickbite.payment.model.PaymentStatusHistory;
import com.quickbite.quickbite.payment.repository.PaymentRepository;
import com.quickbite.quickbite.payment.repository.PaymentStatusHistoryRepository;
import com.quickbite.quickbite.payment.service.strategy.PaymentStrategy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentStatusHistoryRepository paymentStatusHistoryRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final List<PaymentStrategy> strategies;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            PaymentStatusHistoryRepository paymentStatusHistoryRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            CartRepository cartRepository,
            OrderRepository orderRepository,
            List<PaymentStrategy> strategies,
            ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.paymentStatusHistoryRepository = paymentStatusHistoryRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.strategies = strategies;
        this.eventPublisher = eventPublisher;
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
    @Transactional(readOnly = true)
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

    @Override
    @Transactional
    public void handleWebhook(String transactionId, PaymentStatus newStatus) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for transaction " + transactionId));

        PaymentStatus previousStatus = payment.getCurrentStatus();

        // Idempotency: Skip if already in target state
        if (previousStatus.equals(newStatus)) {
            return;
        }

        payment.setCurrentStatus(newStatus);
        paymentRepository.save(payment);

        PaymentStatusHistory history = new PaymentStatusHistory();
        history.setPayment(payment);
        history.setStatus(newStatus);
        paymentStatusHistoryRepository.save(history);

        Order order = payment.getOrder();

        if (newStatus == PaymentStatus.SUCCESS) {
            if (order.getCurrentStatus() == OrderStatus.AWAITING_PAYMENT) {
                order.setCurrentStatus(OrderStatus.PLACED);
                orderRepository.save(order);

                OrderStatusHistory orderHistory = new OrderStatusHistory();
                orderHistory.setOrder(order);
                orderHistory.setOrderStatus(OrderStatus.PLACED);
                orderStatusHistoryRepository.save(orderHistory);

                // Clear customer's active cart
                cartRepository.findByCustomer(order.getCustomer())
                        .ifPresent(cartRepository::delete);

                // Publish OrderPlacedEvent (AFTER_COMMIT Kafka dispatch)
                eventPublisher.publishEvent(new OrderPlacedEvent(
                        order.getId(),
                        order.getCustomer().getId(),
                        order.getCustomer().getName(),
                        order.getCustomer().getEmail(),
                        order.getRestaurant().getId(),
                        order.getRestaurant().getName(),
                        order.getTotalAmount(),
                        order.getCreatedAt()
                ));
            }
        } else if (newStatus == PaymentStatus.FAILED) {
            if (order.getCurrentStatus() == OrderStatus.AWAITING_PAYMENT) {
                order.setCurrentStatus(OrderStatus.PAYMENT_FAILED);
                orderRepository.save(order);

                OrderStatusHistory orderHistory = new OrderStatusHistory();
                orderHistory.setOrder(order);
                orderHistory.setOrderStatus(OrderStatus.PAYMENT_FAILED);
                orderStatusHistoryRepository.save(orderHistory);
            }
        }

        // Publish PaymentStatusChangedEvent (AFTER_COMMIT Kafka dispatch)
        eventPublisher.publishEvent(new PaymentStatusChangedEvent(
                payment.getId(),
                order.getId(),
                order.getCustomer().getId(),
                previousStatus,
                newStatus,
                payment.getPaymentMethod(),
                payment.getAmount(),
                Instant.now()
        ));
    }
}
