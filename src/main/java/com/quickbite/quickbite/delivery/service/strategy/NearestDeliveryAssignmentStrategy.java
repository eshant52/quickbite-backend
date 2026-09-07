package com.quickbite.quickbite.delivery.service.strategy;

import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.order.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class NearestDeliveryAssignmentStrategy implements DeliveryAssignmentStrategy {

    private static final String NAME = "NEAREST";

    private final DeliveryAgentRepository deliveryAgentRepository;

    public NearestDeliveryAssignmentStrategy(DeliveryAgentRepository deliveryAgentRepository) {
        this.deliveryAgentRepository = deliveryAgentRepository;
    }

    @Override
    public Optional<DeliveryAgent> findAgent(Order order) {
        Point refPoint = extractReferencePoint(order);

        if (refPoint == null) {
            log.warn("Cannot find agent: Order {} has no restaurant or delivery location", order.getId());
            return Optional.empty();
        }

        double refLat = refPoint.getY();
        double refLng = refPoint.getX();

        List<DeliveryAgent> nearest = deliveryAgentRepository.findNearestAvailableAgents(refLat, refLng, 1);

        if (nearest.isEmpty()) {
            log.info("No available delivery agents found near ({}, {})", refLat, refLng);
        } else {
            log.info("Found agent {} near ({}, {})", nearest.getFirst().getId(), refLat, refLng);
        }

        return nearest.isEmpty() ? Optional.empty() : Optional.of(nearest.getFirst());
    }

    @Override
    public String strategyName() {
        return NAME;
    }

    private Point extractReferencePoint(Order order) {
        if (order.getRestaurant() != null
                && order.getRestaurant().getAddress() != null
                && order.getRestaurant().getAddress().getLocation() != null) {
            return order.getRestaurant().getAddress().getLocation();
        }
        return order.getDeliveryLocation();
    }
}
