package com.quickbite.quickbite.order.scheduler;

import com.quickbite.quickbite.common.config.property.OrderProperties;
import com.quickbite.quickbite.order.service.OrderLifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderAbandonmentSchedulerTest {

    @Mock private OrderLifecycleService orderLifecycleService;
    @Mock private RedissonClient redissonClient;
    @Mock private RLock rLock;

    private OrderAbandonmentScheduler scheduler;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
        lenient().when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        lenient().when(rLock.isHeldByCurrentThread()).thenReturn(true);

        OrderProperties orderProperties = new OrderProperties(15, 100);
        scheduler = new OrderAbandonmentScheduler(
                orderLifecycleService,
                orderProperties,
                redissonClient
        );
    }

    @Test
    @DisplayName("Single partial batch terminates sweep immediately and releases lock")
    void sweepAbandonedOrders_singlePartialBatch() {
        when(orderLifecycleService.processAbandonmentBatch(any(Instant.class), eq(100)))
                .thenReturn(25);

        scheduler.sweepAbandonedOrders();

        verify(orderLifecycleService, times(1))
                .processAbandonmentBatch(any(Instant.class), eq(100));
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("Multiple batches processed until partial batch received")
    void sweepAbandonedOrders_multiBatch() {
        when(orderLifecycleService.processAbandonmentBatch(any(Instant.class), eq(100)))
                .thenReturn(100) // batch 1
                .thenReturn(42);  // batch 2 (partial -> stop)

        scheduler.sweepAbandonedOrders();

        verify(orderLifecycleService, times(2))
                .processAbandonmentBatch(any(Instant.class), eq(100));
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("Caps at MAX_BATCHES_PER_RUN (10) when full batches keep returning")
    void sweepAbandonedOrders_capsAtMaxBatches() {
        when(orderLifecycleService.processAbandonmentBatch(any(Instant.class), eq(100)))
                .thenReturn(100);

        scheduler.sweepAbandonedOrders();

        verify(orderLifecycleService, times(10))
                .processAbandonmentBatch(any(Instant.class), eq(100));
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("No stale orders terminates after first query")
    void sweepAbandonedOrders_noStaleOrders() {
        when(orderLifecycleService.processAbandonmentBatch(any(Instant.class), eq(100)))
                .thenReturn(0);

        scheduler.sweepAbandonedOrders();

        verify(orderLifecycleService, times(1))
                .processAbandonmentBatch(any(Instant.class), eq(100));
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("Skips sweep when distributed lock is not acquired")
    void sweepAbandonedOrders_whenLockNotAcquired_skips() throws Exception {
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        scheduler.sweepAbandonedOrders();

        verifyNoInteractions(orderLifecycleService);
        verify(rLock, never()).unlock();
    }

    @Test
    @DisplayName("Catches exception during batch processing gracefully and releases lock")
    void sweepAbandonedOrders_handlesException() {
        when(orderLifecycleService.processAbandonmentBatch(any(Instant.class), eq(100)))
                .thenThrow(new RuntimeException("Database timeout"));

        scheduler.sweepAbandonedOrders();

        verify(orderLifecycleService, times(1))
                .processAbandonmentBatch(any(Instant.class), eq(100));
        verify(rLock).unlock();
    }
}
