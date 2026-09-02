package com.quickbite.quickbite.delivery.service.strategy;

import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.order.model.Order;
import org.locationtech.jts.geom.Point;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Primary
public class NearestDeliveryAssignmentStrategy implements DeliveryAssignmentStrategy {

    private final DeliveryAgentRepository deliveryAgentRepository;

    public NearestDeliveryAssignmentStrategy(DeliveryAgentRepository deliveryAgentRepository) {
        this.deliveryAgentRepository = deliveryAgentRepository;
    }

    @Override
    public Optional<DeliveryAgent> findAgent(Order order) {
        Point referencePoint = null;

        if (order.getRestaurant() != null
                && order.getRestaurant().getAddress() != null
                && order.getRestaurant().getAddress().getLocation() != null) {
            referencePoint = order.getRestaurant().getAddress().getLocation();
        } else if (order.getDeliveryLocation() != null) {
            referencePoint = order.getDeliveryLocation();
        }

        if (referencePoint == null) {
            return Optional.empty();
        }

        double lat = referencePoint.getY();
        double lng = referencePoint.getX();

        List<DeliveryAgent> nearest = deliveryAgentRepository.findNearestAvailableAgents(lat, lng, 1);
        return nearest.isEmpty() ? Optional.empty() : Optional.of(nearest.getFirst());
    }

    @Override
    public String strategyName() {
        return "NEAREST";
    }
}
