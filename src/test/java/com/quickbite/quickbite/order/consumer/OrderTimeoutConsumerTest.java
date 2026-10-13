package com.quickbite.quickbite.order.consumer;

import com.quickbite.quickbite.common.config.property.OrderProperties;
import com.quickbite.quickbite.order.service.OrderLifecycleService;
import com.quickbite.quickbite.order.service.OrderTimeoutQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBlockingQueue;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderTimeoutConsumerTest {

    @Mock
    private OrderTimeoutQueueService queueService;

    @Mock
    private OrderLifecycleService orderLifecycleService;

    @Mock
    private RBlockingQueue<UUID> acceptanceQueue;

    @Mock
    private RBlockingQueue<UUID> abandonmentQueue;

    private OrderProperties orderProperties;
    private OrderTimeoutConsumer consumer;

    @BeforeEach
    void setUp() {
        orderProperties = new OrderProperties(
                15, 100, "0 0 * * * *", 5, Duration.ofMinutes(2),
                true, "acceptance-q", "abandonment-q", 1800000L
        );
        consumer = new OrderTimeoutConsumer(queueService, orderLifecycleService, orderProperties);
    }

    @Test
    @DisplayName("processAcceptanceTimeout delegates to cancelUnacceptedOrder")
    void processAcceptanceTimeout_success() {
        UUID orderId = UUID.randomUUID();

        consumer.processAcceptanceTimeout(orderId);

        verify(orderLifecycleService).cancelUnacceptedOrder(orderId);
    }

    @Test
    @DisplayName("processAcceptanceTimeout handles exception gracefully without throwing")
    void processAcceptanceTimeout_handlesException() {
        UUID orderId = UUID.randomUUID();
        doThrow(new RuntimeException("DB lock error")).when(orderLifecycleService).cancelUnacceptedOrder(orderId);

        // Should not throw
        consumer.processAcceptanceTimeout(orderId);

        verify(orderLifecycleService).cancelUnacceptedOrder(orderId);
    }

    @Test
    @DisplayName("processAbandonment delegates to abandonOrderById")
    void processAbandonment_success() {
        UUID orderId = UUID.randomUUID();

        consumer.processAbandonment(orderId);

        verify(orderLifecycleService).abandonOrderById(orderId, "Payment window expired");
    }

    @Test
    @DisplayName("processAbandonment handles exception gracefully without throwing")
    void processAbandonment_handlesException() {
        UUID orderId = UUID.randomUUID();
        doThrow(new RuntimeException("DB error")).when(orderLifecycleService).abandonOrderById(eq(orderId), anyString());

        // Should not throw
        consumer.processAbandonment(orderId);

        verify(orderLifecycleService).abandonOrderById(orderId, "Payment window expired");
    }

    @Test
    @DisplayName("start and stop lifecycle manages running state cleanly")
    void lifecycle_startAndStop() {
        when(queueService.getAcceptanceBlockingQueue()).thenReturn(acceptanceQueue);
        when(queueService.getAbandonmentBlockingQueue()).thenReturn(abandonmentQueue);

        assertThat(consumer.isRunning()).isFalse();

        consumer.start();
        assertThat(consumer.isRunning()).isTrue();

        consumer.stop();
        assertThat(consumer.isRunning()).isFalse();
    }

    @Test
    @DisplayName("start does not start workers when delayedQueueEnabled is false")
    void start_disabled() {
        OrderProperties disabledProps = new OrderProperties(
                15, 100, "0 0 * * * *", 5, Duration.ofMinutes(2),
                false, "acceptance-q", "abandonment-q", 1800000L
        );
        OrderTimeoutConsumer disabledConsumer = new OrderTimeoutConsumer(queueService, orderLifecycleService, disabledProps);

        disabledConsumer.start();

        assertThat(disabledConsumer.isRunning()).isFalse();
        verifyNoInteractions(queueService);
    }

    @Test
    @DisplayName("getPhase returns Integer.MAX_VALUE - 100")
    void getPhase() {
        assertThat(consumer.getPhase()).isEqualTo(Integer.MAX_VALUE - 100);
    }
}
