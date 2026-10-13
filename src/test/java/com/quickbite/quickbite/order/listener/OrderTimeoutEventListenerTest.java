package com.quickbite.quickbite.order.listener;

import com.quickbite.quickbite.common.config.property.OrderProperties;
import com.quickbite.quickbite.common.event.order.OrderPlacedEvent;
import com.quickbite.quickbite.order.service.OrderTimeoutQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderTimeoutEventListenerTest {

    @Mock
    private OrderTimeoutQueueService orderTimeoutQueueService;

    private OrderProperties orderProperties;
    private OrderTimeoutEventListener listener;

    @BeforeEach
    void setUp() {
        orderProperties = new OrderProperties(
                15, 100, "0 0 * * * *", 5, Duration.ofMinutes(2),
                true, "acceptance-q", "abandonment-q", 1800000L
        );
        listener = new OrderTimeoutEventListener(orderTimeoutQueueService, orderProperties);
    }

    @Test
    @DisplayName("onOrderPlaced schedules restaurant acceptance timeout for the order")
    void onOrderPlaced_schedulesTimeout() {
        UUID orderId = UUID.randomUUID();
        OrderPlacedEvent event = new OrderPlacedEvent(
                orderId,
                UUID.randomUUID(),
                "John Doe",
                "john@example.com",
                UUID.randomUUID(),
                "Tasty Bistro",
                BigDecimal.valueOf(250.00),
                Instant.now()
        );

        listener.onOrderPlaced(event);

        verify(orderTimeoutQueueService).scheduleRestaurantAcceptanceTimeout(orderId, Duration.ofMinutes(2));
    }
}
