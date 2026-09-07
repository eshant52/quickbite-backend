package com.quickbite.quickbite.delivery.service.strategy;

import com.quickbite.quickbite.common.config.property.AssignmentProperties;
import com.quickbite.quickbite.common.routing.GeoPoint;
import com.quickbite.quickbite.common.routing.RoutingGateway;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.user.model.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FairNearestDeliveryAssignmentStrategyTest {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Mock
    private DeliveryAgentRepository deliveryAgentRepository;

    @Mock
    private RoutingGateway routingGateway;

    private AssignmentProperties properties;
    private FairNearestDeliveryAssignmentStrategy strategy;

    private Order order;
    private Restaurant restaurant;
    private Address restaurantAddress;

    @BeforeEach
    void setUp() {
        properties = new AssignmentProperties(10, 900);
        strategy = new FairNearestDeliveryAssignmentStrategy(deliveryAgentRepository, routingGateway, properties);

        restaurantAddress = new Address();
        restaurantAddress.setLocation(GF.createPoint(new Coordinate(77.5946, 12.9716))); // lng, lat

        restaurant = new Restaurant();
        restaurant.setId(UUID.randomUUID());
        restaurant.setAddress(restaurantAddress);

        order = new Order();
        order.setId(UUID.randomUUID());
        order.setRestaurant(restaurant);
    }

    @Test
    @DisplayName("Picks driver with lowest road driving duration")
    void findAgent_picksLowestDriveTime() {
        DeliveryAgent agent1 = createAgent(UUID.randomUUID(), 77.6000, 12.9750, null);
        DeliveryAgent agent2 = createAgent(UUID.randomUUID(), 77.5900, 12.9700, null);

        when(deliveryAgentRepository.findNearestAvailableAgents(eq(12.9716), eq(77.5946), eq(10)))
                .thenReturn(List.of(agent1, agent2));

        // Agent1 is 400s away, Agent2 is 250s away
        when(routingGateway.travelTimes(any(GeoPoint.class), any()))
                .thenReturn(List.of(400L, 250L));

        Optional<DeliveryAgent> result = strategy.findAgent(order);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(agent2.getId());
    }

    @Test
    @DisplayName("Breaks ties using lastAssignedAt (null / oldest assigned agent wins)")
    void findAgent_tieBreaksByLastAssignedAt() {
        Instant now = Instant.now();
        Instant older = now.minusSeconds(3600);

        DeliveryAgent agentBusyRecently = createAgent(UUID.randomUUID(), 77.6000, 12.9750, now);
        DeliveryAgent agentWaitingLong = createAgent(UUID.randomUUID(), 77.5900, 12.9700, older);
        DeliveryAgent agentNeverAssigned = createAgent(UUID.randomUUID(), 77.5950, 12.9720, null);

        when(deliveryAgentRepository.findNearestAvailableAgents(eq(12.9716), eq(77.5946), eq(10)))
                .thenReturn(List.of(agentBusyRecently, agentWaitingLong, agentNeverAssigned));

        // All 3 have the same road drive time (300 seconds)
        when(routingGateway.travelTimes(any(GeoPoint.class), any()))
                .thenReturn(List.of(300L, 300L, 300L));

        Optional<DeliveryAgent> result = strategy.findAgent(order);

        assertThat(result).isPresent();
        // Agent never assigned (null) should win over waiting long and busy recently
        assertThat(result.get().getId()).isEqualTo(agentNeverAssigned.getId());
    }

    @Test
    @DisplayName("Returns no assignment when every candidate exceeds the drive-time limit")
    void findAgent_returnsEmptyWhenAllCandidatesExceedDriveTimeLimit() {
        DeliveryAgent firstAgent = createAgent(UUID.randomUUID(), 77.6000, 12.9750, null);
        DeliveryAgent secondAgent = createAgent(UUID.randomUUID(), 77.5900, 12.9700, null);
        when(deliveryAgentRepository.findNearestAvailableAgents(eq(12.9716), eq(77.5946), eq(10)))
                .thenReturn(List.of(firstAgent, secondAgent));
        when(routingGateway.travelTimes(any(GeoPoint.class), any()))
                .thenReturn(List.of(901L, 1_200L));

        Optional<DeliveryAgent> result = strategy.findAgent(order);

        assertThat(result).isEmpty();
    }

    private DeliveryAgent createAgent(UUID id, double lng, double lat, Instant lastAssignedAt) {
        DeliveryAgent da = new DeliveryAgent();
        da.setId(id);
        da.setLastLocation(GF.createPoint(new Coordinate(lng, lat)));
        da.setLastAssignedAt(lastAssignedAt);
        return da;
    }
}
