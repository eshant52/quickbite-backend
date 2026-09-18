package com.quickbite.quickbite.notification.listener;

import com.quickbite.quickbite.common.event.order.OrderCancelledEvent;
import com.quickbite.quickbite.common.event.order.OrderPlacedEvent;
import com.quickbite.quickbite.common.event.order.OrderStatusChangedEvent;
import com.quickbite.quickbite.notification.dto.OrderNotificationPayload;
import com.quickbite.quickbite.notification.model.OrderNotificationType;
import com.quickbite.quickbite.notification.service.NotificationService;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock private NotificationService notificationService;
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private OrderEventListener orderEventListener;

    private User customer;
    private Order order;
    private UUID customerId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        customer = new User();
        customer.setId(customerId);
        customer.setName("Alice");
        customer.setEmail("alice@example.com");

        order = new Order();
        order.setId(orderId);
        order.setCustomer(customer);
        order.setTotalAmount(BigDecimal.valueOf(350.00));
        order.setCurrentStatus(OrderStatus.PLACED);
    }

    @Test
    @DisplayName("onOrderEvent processes OrderPlacedEvent and sends notification")
    void onOrderEvent_orderPlaced() throws Exception {
        OrderPlacedEvent event = new OrderPlacedEvent(
                orderId, customerId, "Alice", "alice@example.com",
                UUID.randomUUID(), "Pizza Co", BigDecimal.valueOf(350.00), Instant.now()
        );
        String rawEvent = "{\"orderId\":\"" + orderId + "\"}";

        when(objectMapper.readValue(rawEvent, com.quickbite.quickbite.common.event.order.OrderEvent.class))
                .thenReturn(event);
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(order));

        orderEventListener.onOrderEvent(rawEvent);

        ArgumentCaptor<OrderNotificationPayload> captor = ArgumentCaptor.forClass(OrderNotificationPayload.class);
        verify(notificationService).send(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(OrderNotificationType.PLACED);
    }

    @Test
    @DisplayName("onOrderEvent processes OrderStatusChangedEvent for ABANDONED status")
    void onOrderEvent_orderAbandoned() throws Exception {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                orderId, customerId, UUID.randomUUID(), OrderStatus.AWAITING_PAYMENT, OrderStatus.ABANDONED, Instant.now()
        );
        String rawEvent = "{\"orderId\":\"" + orderId + "\"}";

        when(objectMapper.readValue(rawEvent, com.quickbite.quickbite.common.event.order.OrderEvent.class))
                .thenReturn(event);
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(order));

        orderEventListener.onOrderEvent(rawEvent);

        ArgumentCaptor<OrderNotificationPayload> captor = ArgumentCaptor.forClass(OrderNotificationPayload.class);
        verify(notificationService).send(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(OrderNotificationType.ABANDONED);
        assertThat(captor.getValue().title()).contains("Inactivity");
    }

    @Test
    @DisplayName("onOrderEvent processes OrderCancelledEvent and sends notification")
    void onOrderEvent_orderCancelled() throws Exception {
        OrderCancelledEvent event = new OrderCancelledEvent(
                orderId, customerId, UUID.randomUUID(), Instant.now()
        );
        String rawEvent = "{\"orderId\":\"" + orderId + "\"}";

        when(objectMapper.readValue(rawEvent, com.quickbite.quickbite.common.event.order.OrderEvent.class))
                .thenReturn(event);
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(order));

        orderEventListener.onOrderEvent(rawEvent);

        ArgumentCaptor<OrderNotificationPayload> captor = ArgumentCaptor.forClass(OrderNotificationPayload.class);
        verify(notificationService).send(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(OrderNotificationType.CANCELLED);
    }
}
