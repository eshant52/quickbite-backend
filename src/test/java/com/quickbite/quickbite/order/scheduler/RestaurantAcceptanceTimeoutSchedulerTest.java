package com.quickbite.quickbite.order.scheduler;

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
class RestaurantAcceptanceTimeoutSchedulerTest {

    @Mock
    private OrderLifecycleService orderLifecycleService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    private RestaurantAcceptanceTimeoutScheduler scheduler;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
        lenient().when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        lenient().when(rLock.isHeldByCurrentThread()).thenReturn(true);

        scheduler = new RestaurantAcceptanceTimeoutScheduler(orderLifecycleService, redissonClient);
    }

    @Test
    @DisplayName("sweepUnacceptedOrders processes batches until processedCount < batchSize")
    void sweepUnacceptedOrders_processesUntilExhausted() {
        when(orderLifecycleService.processRestaurantAcceptanceTimeoutBatch(any(Instant.class), eq(50)))
                .thenReturn(50)
                .thenReturn(20);

        scheduler.sweepUnacceptedOrders();

        verify(orderLifecycleService, times(2)).processRestaurantAcceptanceTimeoutBatch(any(Instant.class), eq(50));
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("sweepUnacceptedOrders stops after one empty batch")
    void sweepUnacceptedOrders_stopsOnZero() {
        when(orderLifecycleService.processRestaurantAcceptanceTimeoutBatch(any(Instant.class), eq(50)))
                .thenReturn(0);

        scheduler.sweepUnacceptedOrders();

        verify(orderLifecycleService, times(1)).processRestaurantAcceptanceTimeoutBatch(any(Instant.class), eq(50));
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("sweepUnacceptedOrders skips processing when distributed lock is not acquired")
    void sweepUnacceptedOrders_whenLockNotAcquired_skips() throws Exception {
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        scheduler.sweepUnacceptedOrders();

        verifyNoInteractions(orderLifecycleService);
        verify(rLock, never()).unlock();
    }

    @Test
    @DisplayName("sweepUnacceptedOrders handles exceptions gracefully and releases lock")
    void sweepUnacceptedOrders_handlesException() {
        when(orderLifecycleService.processRestaurantAcceptanceTimeoutBatch(any(Instant.class), eq(50)))
                .thenThrow(new RuntimeException("DB error"));

        // Should not throw
        scheduler.sweepUnacceptedOrders();

        verify(orderLifecycleService, times(1)).processRestaurantAcceptanceTimeoutBatch(any(Instant.class), eq(50));
        verify(rLock).unlock();
    }
}
