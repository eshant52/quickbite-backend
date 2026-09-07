package com.quickbite.quickbite.delivery.service.strategy;

import com.quickbite.quickbite.common.config.property.AssignmentProperties;
import com.quickbite.quickbite.common.routing.GeoPoint;
import com.quickbite.quickbite.common.routing.RoutingGateway;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.order.model.Order;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Fair + Nearest Delivery Agent Assignment Strategy.
 *
 * <p>Uses a two-tier dispatch algorithm:
 * <ol>
 *   <li><b>Tier 1 (PostGIS):</b> Coarsely filters the top candidate pool (e.g. 10) of active,
 *       approved, available agents using the spatial GiST index on {@code last_location}.</li>
 *   <li><b>Tier 2 (RoutingGateway - OSRM / Mapbox / Haversine):</b> Calculates actual road network
 *       driving travel times from each candidate's location to the restaurant.</li>
 *   <li><b>Fairness Tiebreaker:</b> Agents with similar drive times are tie-broken by
 *       {@code lastAssignedAt ASC} (nulls first), guaranteeing starvation-free rotation.</li>
 * </ol>
 */
@Component
@Primary
public class FairNearestDeliveryAssignmentStrategy implements DeliveryAssignmentStrategy {

    private static final String NAME = "FAIR_NEAREST";

    private static final Logger log = LoggerFactory.getLogger(FairNearestDeliveryAssignmentStrategy.class);

    private final DeliveryAgentRepository deliveryAgentRepository;
    private final RoutingGateway routingGateway;
    private final AssignmentProperties properties;

    public FairNearestDeliveryAssignmentStrategy(
            DeliveryAgentRepository deliveryAgentRepository,
            RoutingGateway routingGateway,
            AssignmentProperties properties
    ) {
        this.deliveryAgentRepository = deliveryAgentRepository;
        this.routingGateway = routingGateway;
        this.properties = properties;
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

        // Tier 1: PostGIS coarse candidate pool
        List<DeliveryAgent> candidates = deliveryAgentRepository.findNearestAvailableAgents(
                refLat, refLng, properties.candidatePoolSize()
        );

        if (candidates.isEmpty()) {
            log.info("No available delivery agents found near ({}, {})", refLat, refLng);
            return Optional.empty();
        }

        if (candidates.size() == 1) {
            log.info("Only one candidate found for order {}", order.getId());
            return Optional.of(candidates.getFirst());
        }

        // Tier 2: Extract candidate coordinates
        List<GeoPoint> candidateLocations = new ArrayList<>(candidates.size());
        for (DeliveryAgent agent : candidates) {
            Point loc = agent.getLastLocation();
            candidateLocations.add(GeoPoint.of(loc.getY(), loc.getX()));
        }

        // OSRM / Mapbox Matrix API for driving duration to restaurant
        GeoPoint refGeo = GeoPoint.of(refLat, refLng);
        List<Long> durations = routingGateway.travelTimes(refGeo, candidateLocations);

        // Build candidate evaluation list
        List<CandidateEvaluation> evaluations = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            long duration = (i < durations.size() && durations.get(i) != null)
                    ? durations.get(i)
                    : Long.MAX_VALUE;
            evaluations.add(new CandidateEvaluation(candidates.get(i), duration));
        }

        // Sort by:
        // 1. Duration (ascending)
        // 2. lastAssignedAt (ascending, nulls first - agents who haven't received an order get priority)
        evaluations.sort(Comparator
                .comparingLong(CandidateEvaluation::durationSeconds)
                .thenComparing(
                        eval -> eval.agent().getLastAssignedAt(),
                        Comparator.nullsFirst(Comparator.naturalOrder())
                )
        );

        CandidateEvaluation selected = evaluations.stream()
                .filter(evaluation -> evaluation.durationSeconds() <= properties.maxAcceptableDriveSeconds())
                .findFirst()
                .orElse(null);
        if (selected == null) {
            log.info("No candidate is within the maximum acceptable drive time of {}s",
                    properties.maxAcceptableDriveSeconds());
            return Optional.empty();
        }

        DeliveryAgent chosen = selected.agent();
        log.info("Assigned agent {} with estimated road drive time {}s (last assigned: {})",
                chosen.getId(), selected.durationSeconds(), chosen.getLastAssignedAt());

        return Optional.of(chosen);
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

    private record CandidateEvaluation(
            DeliveryAgent agent,
            long durationSeconds
    ) {}
}
