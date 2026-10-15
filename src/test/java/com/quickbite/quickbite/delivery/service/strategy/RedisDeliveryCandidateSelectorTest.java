package com.quickbite.quickbite.delivery.service.strategy;

import com.quickbite.quickbite.common.config.property.DeliveryGeoProperties;
import com.quickbite.quickbite.common.config.property.DispatchProperties;
import com.quickbite.quickbite.common.routing.GeoPoint;
import com.quickbite.quickbite.common.routing.RoutingGateway;
import com.quickbite.quickbite.delivery.dto.DriverGeoResult;
import com.quickbite.quickbite.delivery.dto.GeoCoordinate;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.model.DeliveryAgentVerificationStatus;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.delivery.service.DeliveryTrackingService;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.vehicle.model.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisDeliveryCandidateSelectorTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Mock
    private DeliveryTrackingService deliveryTrackingService;

    @Mock
    private PostgisDeliveryCandidateSelector postgisCandidateSelector;

    @Mock
    private DeliveryAgentRepository deliveryAgentRepository;

    @Mock
    private RoutingGateway routingGateway;

    private DispatchProperties dispatchProperties;
    private DeliveryGeoProperties deliveryGeoProperties;
    private RedisDeliveryCandidateSelector selector;

    private Order order;
    private Point restaurantLocation;

    @BeforeEach
    void setUp() {
        dispatchProperties = new DispatchProperties(null, null, null, null, 10, null);
        deliveryGeoProperties = new DeliveryGeoProperties(true, Duration.ofSeconds(60), Duration.ofSeconds(30));
        selector = new RedisDeliveryCandidateSelector(
                deliveryTrackingService,
                postgisCandidateSelector,
                deliveryAgentRepository,
                routingGateway,
                dispatchProperties,
                deliveryGeoProperties
        );

        restaurantLocation = GEOMETRY_FACTORY.createPoint(new Coordinate(77.5946, 12.9716));
        Address restaurantAddress = new Address();
        restaurantAddress.setLocation(restaurantLocation);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(UUID.randomUUID());
        restaurant.setAddress(restaurantAddress);

        order = new Order();
        order.setId(UUID.randomUUID());
        order.setRestaurant(restaurant);
    }

    @Test
    @DisplayName("Delegates to PostGIS when Redis GEO is disabled")
    void selectNextCandidate_whenGeoDisabled_delegatesToPostgis() {
        DeliveryGeoProperties disabledProperties = new DeliveryGeoProperties(false, null, null);
        RedisDeliveryCandidateSelector disabledSelector = new RedisDeliveryCandidateSelector(
                deliveryTrackingService,
                postgisCandidateSelector,
                deliveryAgentRepository,
                routingGateway,
                dispatchProperties,
                disabledProperties
        );

        DeliveryAgent agent = new DeliveryAgent();
        when(postgisCandidateSelector.selectNextCandidate(order, 5.0, Collections.emptySet()))
                .thenReturn(Optional.of(agent));

        Optional<DeliveryAgent> result = disabledSelector.selectNextCandidate(order, 5.0, Collections.emptySet());

        assertThat(result).contains(agent);
        verifyNoInteractions(deliveryTrackingService);
    }

    @Test
    @DisplayName("Falls back to PostGIS when Redis returns no candidate drivers")
    void selectNextCandidate_whenRedisEmpty_fallsBackToPostgis() {
        when(deliveryTrackingService.findNearbyAgents(eq(12.9716), eq(77.5946), eq(5.0), anyInt()))
                .thenReturn(Collections.emptyList());

        DeliveryAgent fallbackAgent = new DeliveryAgent();
        when(postgisCandidateSelector.selectNextCandidate(order, 5.0, Collections.emptySet()))
                .thenReturn(Optional.of(fallbackAgent));

        Optional<DeliveryAgent> result = selector.selectNextCandidate(order, 5.0, Collections.emptySet());

        assertThat(result).contains(fallbackAgent);
        verify(postgisCandidateSelector).selectNextCandidate(order, 5.0, Collections.emptySet());
    }

    @Test
    @DisplayName("Filters dead drivers whose heartbeat expired and falls back to PostGIS if none left")
    void selectNextCandidate_filtersDeadDrivers() {
        UUID agentId = UUID.randomUUID();
        DriverGeoResult deadDriver = new DriverGeoResult(agentId, 1.0, GeoCoordinate.of(12.9720, 77.5950));

        when(deliveryTrackingService.findNearbyAgents(anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(deadDriver));
        when(deliveryTrackingService.isAgentAlive(agentId)).thenReturn(false);

        DeliveryAgent fallbackAgent = new DeliveryAgent();
        when(postgisCandidateSelector.selectNextCandidate(order, 5.0, Collections.emptySet()))
                .thenReturn(Optional.of(fallbackAgent));

        Optional<DeliveryAgent> result = selector.selectNextCandidate(order, 5.0, Collections.emptySet());

        assertThat(result).contains(fallbackAgent);
    }

    @Test
    @DisplayName("Selects closest driver by road travel duration and breaks ties fairly by lastAssignedAt")
    void selectNextCandidate_ranksByDurationAndFairness() {
        UUID agent1Id = UUID.randomUUID();
        UUID agent2Id = UUID.randomUUID();

        DriverGeoResult res1 = new DriverGeoResult(agent1Id, 1.5, GeoCoordinate.of(12.9730, 77.5960));
        DriverGeoResult res2 = new DriverGeoResult(agent2Id, 1.0, GeoCoordinate.of(12.9720, 77.5950));

        when(deliveryTrackingService.findNearbyAgents(anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(res1, res2));
        when(deliveryTrackingService.isAgentAlive(agent1Id)).thenReturn(true);
        when(deliveryTrackingService.isAgentAlive(agent2Id)).thenReturn(true);

        Vehicle vehicle = new Vehicle();
        DeliveryAgent agent1 = new DeliveryAgent();
        agent1.setId(agent1Id);
        agent1.setAvailable(true);
        agent1.setAssigned(false);
        agent1.setCurrentStatus(DeliveryAgentVerificationStatus.APPROVED);
        agent1.setCurrentVehicle(vehicle);
        agent1.setLastLocation(GEOMETRY_FACTORY.createPoint(new Coordinate(77.5960, 12.9730)));
        agent1.setLastAssignedAt(Instant.now().minusSeconds(1000));

        DeliveryAgent agent2 = new DeliveryAgent();
        agent2.setId(agent2Id);
        agent2.setAvailable(true);
        agent2.setAssigned(false);
        agent2.setCurrentStatus(DeliveryAgentVerificationStatus.APPROVED);
        agent2.setCurrentVehicle(vehicle);
        agent2.setLastLocation(GEOMETRY_FACTORY.createPoint(new Coordinate(77.5950, 12.9720)));
        agent2.setLastAssignedAt(Instant.now().minusSeconds(500));

        when(deliveryAgentRepository.findAllById(List.of(agent1Id, agent2Id)))
                .thenReturn(List.of(agent1, agent2));

        // Travel durations: agent1 is faster (180s) than agent2 (300s)
        when(routingGateway.travelTimes(any(GeoPoint.class), anyList()))
                .thenReturn(List.of(180L, 300L));

        Optional<DeliveryAgent> selected = selector.selectNextCandidate(order, 5.0, Collections.emptySet());

        assertThat(selected).isPresent();
        assertThat(selected.get().getId()).isEqualTo(agent1Id);
    }
}
