package com.quickbite.quickbite.delivery.service.strategy;

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
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NearestDeliveryAssignmentStrategyTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Mock
    private DeliveryAgentRepository deliveryAgentRepository;

    @InjectMocks
    private NearestDeliveryAssignmentStrategy strategy;

    private Order order;
    private DeliveryAgent nearestAgent;

    @BeforeEach
    void setUp() {
        nearestAgent = new DeliveryAgent();
        nearestAgent.setId(UUID.randomUUID());

        Point restaurantLocation = GEOMETRY_FACTORY.createPoint(new Coordinate(77.5946, 12.9716));
        Address restaurantAddress = new Address();
        restaurantAddress.setLocation(restaurantLocation);

        Restaurant restaurant = new Restaurant();
        restaurant.setAddress(restaurantAddress);

        order = new Order();
        order.setId(UUID.randomUUID());
        order.setRestaurant(restaurant);
    }

    @Test
    @DisplayName("findAgent returns nearest agent using restaurant pickup location")
    void findAgent_fromRestaurantLocation() {
        when(deliveryAgentRepository.findNearestAvailableAgents(eq(12.9716), eq(77.5946), eq(1)))
                .thenReturn(List.of(nearestAgent));

        Optional<DeliveryAgent> result = strategy.findAgent(order);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(nearestAgent);
        assertThat(strategy.strategyName()).isEqualTo("NEAREST");
    }

    @Test
    @DisplayName("findAgent falls back to delivery location if restaurant location is missing")
    void findAgent_fallbackToDeliveryLocation() {
        order.getRestaurant().getAddress().setLocation(null);
        Point deliveryLocation = GEOMETRY_FACTORY.createPoint(new Coordinate(77.6000, 12.9800));
        order.setDeliveryLocation(deliveryLocation);

        when(deliveryAgentRepository.findNearestAvailableAgents(eq(12.9800), eq(77.6000), eq(1)))
                .thenReturn(List.of(nearestAgent));

        Optional<DeliveryAgent> result = strategy.findAgent(order);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(nearestAgent);
    }

    @Test
    @DisplayName("findAgent returns empty optional when no agents available")
    void findAgent_noneAvailable() {
        when(deliveryAgentRepository.findNearestAvailableAgents(anyDouble(), anyDouble(), eq(1)))
                .thenReturn(List.of());

        Optional<DeliveryAgent> result = strategy.findAgent(order);

        assertThat(result).isEmpty();
    }
}
