package com.quickbite.quickbite.delivery.scheduler;

import com.quickbite.quickbite.delivery.model.DeliveryDispatchStatus;
import com.quickbite.quickbite.delivery.model.OrderDispatch;
import com.quickbite.quickbite.delivery.repository.OrderDispatchRepository;
import com.quickbite.quickbite.delivery.service.DeliveryDispatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryDispatchSchedulerTest {

    @Mock
    private OrderDispatchRepository orderDispatchRepository;

    @Mock
    private DeliveryDispatchService deliveryDispatchService;

    private DeliveryDispatchScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new DeliveryDispatchScheduler(orderDispatchRepository, deliveryDispatchService);
    }

    @Test
    @DisplayName("sweepDueDispatches iterates over due dispatches and delegates to service")
    void sweepDueDispatches_delegatesToService() {
        UUID order1 = UUID.randomUUID();
        UUID order2 = UUID.randomUUID();

        when(orderDispatchRepository.findDueDispatches(any(Instant.class), eq(50)))
                .thenReturn(List.of(order1, order2));

        scheduler.sweepDueDispatches();

        verify(deliveryDispatchService).processDueDispatch(order1);
        verify(deliveryDispatchService).processDueDispatch(order2);
    }

    @Test
    @DisplayName("sweepDueDispatches gracefully continues on individual failure")
    void sweepDueDispatches_continuesOnIndividualError() {
        UUID order1 = UUID.randomUUID();
        UUID order2 = UUID.randomUUID();

        when(orderDispatchRepository.findDueDispatches(any(Instant.class), eq(50)))
                .thenReturn(List.of(order1, order2));

        doThrow(new RuntimeException("Lock failure")).when(deliveryDispatchService).processDueDispatch(order1);

        scheduler.sweepDueDispatches();

        verify(deliveryDispatchService).processDueDispatch(order1);
        verify(deliveryDispatchService).processDueDispatch(order2);
    }
}
