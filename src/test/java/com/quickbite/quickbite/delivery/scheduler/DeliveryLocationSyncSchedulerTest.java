package com.quickbite.quickbite.delivery.scheduler;

import com.quickbite.quickbite.common.config.property.DeliveryGeoProperties;
import com.quickbite.quickbite.delivery.dto.GeoCoordinate;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.delivery.service.DeliveryTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryLocationSyncSchedulerTest {

    @Mock
    private DeliveryTrackingService deliveryTrackingService;

    @Mock
    private DeliveryAgentRepository deliveryAgentRepository;

    private DeliveryGeoProperties properties;
    private DeliveryLocationSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new DeliveryGeoProperties(true, Duration.ofSeconds(60), Duration.ofSeconds(30));
        scheduler = new DeliveryLocationSyncScheduler(deliveryTrackingService, deliveryAgentRepository, properties);
    }

    @Test
    @DisplayName("Does nothing when geo tracking is disabled")
    void syncLocations_whenDisabled_doesNothing() {
        DeliveryGeoProperties disabledProps = new DeliveryGeoProperties(false, null, null);
        DeliveryLocationSyncScheduler disabledScheduler =
                new DeliveryLocationSyncScheduler(deliveryTrackingService, deliveryAgentRepository, disabledProps);

        disabledScheduler.syncLocationsToDatabase();

        verifyNoInteractions(deliveryTrackingService);
        verifyNoInteractions(deliveryAgentRepository);
    }

    @Test
    @DisplayName("Does nothing when no active driver locations in Redis")
    void syncLocations_whenEmpty_doesNothing() {
        when(deliveryTrackingService.getAllActiveLocations()).thenReturn(Collections.emptyMap());

        scheduler.syncLocationsToDatabase();

        verify(deliveryTrackingService).getAllActiveLocations();
        verifyNoInteractions(deliveryAgentRepository);
    }

    @Test
    @DisplayName("Flushes active driver locations to PostgreSQL in a single batch")
    void syncLocations_batchesSaveToDatabase() {
        UUID agentId = UUID.randomUUID();
        GeoCoordinate coord = new GeoCoordinate(12.9716, 77.5946);

        when(deliveryTrackingService.getAllActiveLocations()).thenReturn(Map.of(agentId, coord));

        DeliveryAgent agent = new DeliveryAgent();
        agent.setId(agentId);

        when(deliveryAgentRepository.findAllById(anySet())).thenReturn(List.of(agent));

        scheduler.syncLocationsToDatabase();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DeliveryAgent>> captor = ArgumentCaptor.forClass(List.class);
        verify(deliveryAgentRepository).saveAll(captor.capture());

        List<DeliveryAgent> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getLastLocation()).isNotNull();
        assertThat(saved.getFirst().getLastLocation().getY()).isEqualTo(12.9716);
        assertThat(saved.getFirst().getLastLocation().getX()).isEqualTo(77.5946);
    }
}
