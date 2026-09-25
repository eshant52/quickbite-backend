package com.quickbite.quickbite.delivery.service.strategy;

import com.quickbite.quickbite.common.config.property.DispatchProperties;
import com.quickbite.quickbite.common.routing.GeoPoint;
import com.quickbite.quickbite.common.routing.RoutingGateway;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.order.model.Order;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PostgisDeliveryCandidateSelector implements DeliveryCandidateSelector {

    private static final Logger log = LoggerFactory.getLogger(PostgisDeliveryCandidateSelector.class);

    private final DeliveryAgentRepository deliveryAgentRepository;
    private final RoutingGateway routingGateway;
    private final DispatchProperties dispatchProperties;

    public PostgisDeliveryCandidateSelector(
            DeliveryAgentRepository deliveryAgentRepository,
            RoutingGateway routingGateway,
            DispatchProperties dispatchProperties) {
        this.deliveryAgentRepository = deliveryAgentRepository;
        this.routingGateway = routingGateway;
        this.dispatchProperties = dispatchProperties;
    }

    @Override
    public Optional<DeliveryAgent> selectNextCandidate(Order order, double radiusKm, Set<UUID> excludedAgentIds) {
        Point refPoint = extractReferencePoint(order);
        if (refPoint == null) {
            log.warn("Cannot select candidate: Order {} has no restaurant or delivery location", order.getId());
            return Optional.empty();
        }

        double refLat = refPoint.getY();
        double refLng = refPoint.getX();
        double radiusMeters = radiusKm * 1000.0;

        List<DeliveryAgent> pool = deliveryAgentRepository.findNearestAvailableAgentsWithinRadius(
                refLat, refLng, radiusMeters, dispatchProperties.candidateLimit()
        );

        List<DeliveryAgent> eligible = pool.stream()
                .filter(a -> excludedAgentIds == null || !excludedAgentIds.contains(a.getId()))
                .toList();

        if (eligible.isEmpty()) {
            return Optional.empty();
        }

        if (eligible.size() == 1) {
            return Optional.of(eligible.getFirst());
        }

        // Evaluate road travel times via routing gateway
        List<GeoPoint> candidateLocations = eligible.stream()
                .map(a -> GeoPoint.of(a.getLastLocation().getY(), a.getLastLocation().getX()))
                .toList();

        GeoPoint refGeo = GeoPoint.of(refLat, refLng);
        List<Long> durations = routingGateway.travelTimes(refGeo, candidateLocations);

        List<CandidateEvaluation> evaluations = new ArrayList<>(eligible.size());
        for (int i = 0; i < eligible.size(); i++) {
            long duration = (i < durations.size() && durations.get(i) != null)
                    ? durations.get(i)
                    : Long.MAX_VALUE;
            evaluations.add(new CandidateEvaluation(eligible.get(i), duration));
        }

        // Sort by duration ASC, then lastAssignedAt ASC (nulls first)
        evaluations.sort(Comparator
                .comparingLong(CandidateEvaluation::durationSeconds)
                .thenComparing(
                        eval -> eval.agent().getLastAssignedAt(),
                        Comparator.nullsFirst(Comparator.naturalOrder())
                )
        );

        return Optional.of(evaluations.getFirst().agent());
    }

    private Point extractReferencePoint(Order order) {
        if (order.getRestaurant() != null
                && order.getRestaurant().getAddress() != null
                && order.getRestaurant().getAddress().getLocation() != null) {
            return order.getRestaurant().getAddress().getLocation();
        }
        UUID restaurantId = order.getRestaurant() != null ? order.getRestaurant().getId() : null;
        log.warn("Restaurant {} for order {} has no pickup coordinates configured", restaurantId, order.getId());
        return null;
    }

    private record CandidateEvaluation(
            DeliveryAgent agent,
            long durationSeconds
    ) {}
}
