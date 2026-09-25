package com.quickbite.quickbite.order.scheduler;

import com.quickbite.quickbite.order.service.OrderLifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantAcceptanceTimeoutSchedulerTest {

    @Mock
    private OrderLifecycleService orderLifecycleService;

    private RestaurantAcceptanceTimeoutScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new RestaurantAcceptanceTimeoutScheduler(orderLifecycleService);
    }

    @Test
    @DisplayName("sweepUnacceptedOrders processes batches until processedCount < batchSize")
    void sweepUnacceptedOrders_processesUntilExhausted() {
        when(orderLifecycleService.processRestaurantAcceptanceTimeoutBatch(any(Instant.class), eq(50)))
                .thenReturn(50)
                .thenReturn(20);

        scheduler.sweepUnacceptedOrders();

        verify(orderLifecycleService, times(2)).processRestaurantAcceptanceTimeoutBatch(any(Instant.class), eq(50));
    }

    @Test
    @DisplayName("sweepUnacceptedOrders stops after one empty batch")
    void sweepUnacceptedOrders_stopsOnZero() {
        when(orderLifecycleService.processRestaurantAcceptanceTimeoutBatch(any(Instant.class), eq(50)))
                .thenReturn(0);

        scheduler.sweepUnacceptedOrders();

        verify(orderLifecycleService, times(1)).processRestaurantAcceptanceTimeoutBatch(any(Instant.class), eq(50));
    }

    @Test
    @DisplayName("sweepUnacceptedOrders handles exceptions gracefully without rethrowing")
    void sweepUnacceptedOrders_handlesException() {
        when(orderLifecycleService.processRestaurantAcceptanceTimeoutBatch(any(Instant.class), eq(50)))
                .thenThrow(new RuntimeException("DB error"));

        // Should not throw
        scheduler.sweepUnacceptedOrders();

        verify(orderLifecycleService, times(1)).processRestaurantAcceptanceTimeoutBatch(any(Instant.class), eq(50));
    }
}
