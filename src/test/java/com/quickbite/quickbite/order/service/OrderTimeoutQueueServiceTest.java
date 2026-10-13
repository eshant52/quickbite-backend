package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.common.config.property.OrderProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("deprecation")
class OrderTimeoutQueueServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBlockingQueue<UUID> acceptanceBlockingQueue;

    @Mock
    private RDelayedQueue<UUID> acceptanceDelayedQueue;

    @Mock
    private RBlockingQueue<UUID> abandonmentBlockingQueue;

    @Mock
    private RDelayedQueue<UUID> abandonmentDelayedQueue;

    private OrderProperties orderProperties;
    private OrderTimeoutQueueService queueService;

    @BeforeEach
    void setUp() {
        orderProperties = new OrderProperties(
                15, 100, "0 0 * * * *", 5, Duration.ofMinutes(2),
                true, "acceptance-q", "abandonment-q", 1800000L
        );

        when(redissonClient.<UUID>getBlockingQueue("acceptance-q")).thenReturn(acceptanceBlockingQueue);
        when(redissonClient.<UUID>getDelayedQueue(acceptanceBlockingQueue)).thenReturn(acceptanceDelayedQueue);

        when(redissonClient.<UUID>getBlockingQueue("abandonment-q")).thenReturn(abandonmentBlockingQueue);
        when(redissonClient.<UUID>getDelayedQueue(abandonmentBlockingQueue)).thenReturn(abandonmentDelayedQueue);

        queueService = new OrderTimeoutQueueService(redissonClient, orderProperties);
    }

    @Test
    @DisplayName("scheduleRestaurantAcceptanceTimeout enqueues order ID with delay in seconds")
    void scheduleRestaurantAcceptanceTimeout_success() {
        UUID orderId = UUID.randomUUID();
        Duration delay = Duration.ofMinutes(2);

        queueService.scheduleRestaurantAcceptanceTimeout(orderId, delay);

        verify(acceptanceDelayedQueue).offer(orderId, 120L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("scheduleOrderAbandonment enqueues order ID with delay in seconds")
    void scheduleOrderAbandonment_success() {
        UUID orderId = UUID.randomUUID();
        Duration delay = Duration.ofMinutes(15);

        queueService.scheduleOrderAbandonment(orderId, delay);

        verify(abandonmentDelayedQueue).offer(orderId, 900L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("schedule does nothing when delayedQueueEnabled is false")
    void schedule_disabled() {
        OrderProperties disabledProps = new OrderProperties(
                15, 100, "0 0 * * * *", 5, Duration.ofMinutes(2),
                false, "acceptance-q", "abandonment-q", 1800000L
        );
        OrderTimeoutQueueService disabledService = new OrderTimeoutQueueService(redissonClient, disabledProps);

        UUID orderId = UUID.randomUUID();
        disabledService.scheduleRestaurantAcceptanceTimeout(orderId, Duration.ofMinutes(2));
        disabledService.scheduleOrderAbandonment(orderId, Duration.ofMinutes(15));

        verify(acceptanceDelayedQueue, never()).offer(any(), anyLong(), any());
        verify(abandonmentDelayedQueue, never()).offer(any(), anyLong(), any());
    }

    @Test
    @DisplayName("getters return the underlying blocking and delayed queues")
    void getters_returnQueues() {
        assertThat(queueService.getAcceptanceBlockingQueue()).isSameAs(acceptanceBlockingQueue);
        assertThat(queueService.getAcceptanceDelayedQueue()).isSameAs(acceptanceDelayedQueue);
        assertThat(queueService.getAbandonmentBlockingQueue()).isSameAs(abandonmentBlockingQueue);
        assertThat(queueService.getAbandonmentDelayedQueue()).isSameAs(abandonmentDelayedQueue);
    }
}
