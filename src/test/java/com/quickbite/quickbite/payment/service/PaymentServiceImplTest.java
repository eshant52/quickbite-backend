package com.quickbite.quickbite.payment.service;

import com.quickbite.quickbite.cart.model.Cart;
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
import com.quickbite.quickbite.payment.dto.CodPaymentResult;
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
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentStatusHistoryRepository paymentStatusHistoryRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentStrategy paymentStrategy;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PaymentServiceImpl paymentService;

    private User customer;
    private Restaurant restaurant;
    private Order order;
    private Payment payment;
    private UUID customerId;
    private UUID orderId;
    private UUID paymentId;
    private String transactionId;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                paymentStatusHistoryRepository,
                orderStatusHistoryRepository,
                cartRepository,
                orderRepository,
                List.of(paymentStrategy),
                eventPublisher
        );

        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        transactionId = "STUB-12345678";

        customer = new User();
        customer.setId(customerId);
        customer.setName("John Doe");
        customer.setEmail("john@example.com");

        restaurant = new Restaurant();
        restaurant.setId(UUID.randomUUID());
        restaurant.setName("Pasta Palace");

        order = new Order();
        order.setId(orderId);
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setTotalAmount(BigDecimal.valueOf(499.00));
        order.setCurrentStatus(OrderStatus.AWAITING_PAYMENT);
        order.setCreatedAt(Instant.now());

        payment = new Payment();
        payment.setId(paymentId);
        payment.setOrder(order);
        payment.setPaymentMethod(PaymentMethod.UPI);
        payment.setTransactionId(transactionId);
        payment.setAmount(BigDecimal.valueOf(499.00));
        payment.setCurrentStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(Instant.now());
    }

    @Nested
    @DisplayName("initiatePayment")
    class InitiatePaymentTests {

        @Test
        @DisplayName("Dispatches to matching strategy")
        void initiatePayment_success() {
            when(paymentStrategy.supports(PaymentMethod.COD)).thenReturn(true);
            PaymentResult mockResult = new CodPaymentResult(paymentId, orderId, transactionId, BigDecimal.valueOf(499.00));
            when(paymentStrategy.initiate(order, PaymentMethod.COD)).thenReturn(mockResult);

            PaymentResult result = paymentService.initiatePayment(order, PaymentMethod.COD);

            assertThat(result).isEqualTo(mockResult);
        }

        @Test
        @DisplayName("Throws BadRequestException when no strategy supports method")
        void initiatePayment_unsupported() {
            when(paymentStrategy.supports(PaymentMethod.WALLET)).thenReturn(false);

            assertThatThrownBy(() -> paymentService.initiatePayment(order, PaymentMethod.WALLET))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Unsupported payment method");
        }
    }

    @Nested
    @DisplayName("getPaymentByOrderId")
    class GetPaymentByOrderIdTests {

        @Test
        @DisplayName("Returns PaymentResponse when order and payment exist")
        void getPaymentByOrderId_success() {
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(order));
            when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

            PaymentResponse response = paymentService.getPaymentByOrderId(orderId, customerId);

            assertThat(response.id()).isEqualTo(paymentId);
            assertThat(response.orderId()).isEqualTo(orderId);
            assertThat(response.method()).isEqualTo(PaymentMethod.UPI);
            assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when order does not belong to customer")
        void getPaymentByOrderId_orderNotFound() {
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentByOrderId(orderId, customerId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Order not found");
        }
    }

    @Nested
    @DisplayName("handleWebhook")
    class HandleWebhookTests {

        @Test
        @DisplayName("Transitions payment to SUCCESS, order to PLACED, deletes cart, and publishes events")
        void handleWebhook_success() {
            when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(payment));
            Cart cart = new Cart();
            when(cartRepository.findByCustomer(customer)).thenReturn(Optional.of(cart));

            paymentService.handleWebhook(transactionId, PaymentStatus.SUCCESS);

            assertThat(payment.getCurrentStatus()).isEqualTo(PaymentStatus.SUCCESS);
            verify(paymentRepository).save(payment);
            verify(paymentStatusHistoryRepository).save(any(PaymentStatusHistory.class));

            assertThat(order.getCurrentStatus()).isEqualTo(OrderStatus.PLACED);
            verify(orderRepository).save(order);
            verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
            verify(cartRepository).delete(cart);

            verify(eventPublisher).publishEvent(any(OrderPlacedEvent.class));
            verify(eventPublisher).publishEvent(any(PaymentStatusChangedEvent.class));
        }

        @Test
        @DisplayName("Transitions payment to FAILED, order to PAYMENT_FAILED, and publishes PaymentStatusChangedEvent")
        void handleWebhook_failed() {
            when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(payment));

            paymentService.handleWebhook(transactionId, PaymentStatus.FAILED);

            assertThat(payment.getCurrentStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(order.getCurrentStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);

            verify(orderRepository).save(order);
            verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
            verifyNoInteractions(cartRepository);

            verify(eventPublisher, never()).publishEvent(any(OrderPlacedEvent.class));
            verify(eventPublisher).publishEvent(any(PaymentStatusChangedEvent.class));
        }

        @Test
        @DisplayName("Idempotent: returns immediately if payment is already in target status")
        void handleWebhook_idempotent() {
            payment.setCurrentStatus(PaymentStatus.SUCCESS);
            when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(payment));

            paymentService.handleWebhook(transactionId, PaymentStatus.SUCCESS);

            verify(paymentRepository, never()).save(any());
            verify(orderRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Throws PaymentNotFoundException when transactionId does not exist")
        void handleWebhook_notFound() {
            when(paymentRepository.findByTransactionId("INVALID-TXN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.handleWebhook("INVALID-TXN", PaymentStatus.SUCCESS))
                    .isInstanceOf(PaymentNotFoundException.class);
        }
    }
}
