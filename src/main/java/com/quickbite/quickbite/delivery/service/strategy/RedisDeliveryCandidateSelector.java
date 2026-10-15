package com.quickbite.quickbite.delivery.service.strategy;

import com.quickbite.quickbite.common.config.property.DeliveryGeoProperties;
import com.quickbite.quickbite.common.config.property.DispatchProperties;
import com.quickbite.quickbite.common.routing.GeoPoint;
import com.quickbite.quickbite.common.routing.RoutingGateway;
import com.quickbite.quickbite.delivery.dto.DriverGeoResult;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.model.DeliveryAgentVerificationStatus;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.delivery.service.DeliveryTrackingService;
import com.quickbite.quickbite.order.model.Order;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * High-performance candidate selector for proactive delivery dispatch using in-memory Redis GEO.
 *
 * <p>Flow:
 * <ol>
 *     <li>Queries candidate drivers in RAM within {@code radiusKm} using {@code GEOSEARCH}.</li>
 *     <li>Verifies active 60s heartbeats to filter out dead/disconnected ghost drivers.</li>
 *     <li>Hydrates and verifies current duty/assignment state from PostgreSQL.</li>
 *     <li>Calculates road driving durations via {@link RoutingGateway} and breaks ties fairly by {@code lastAssignedAt}.</li>
 *     <li>Falls back to {@link PostgisDeliveryCandidateSelector} if Redis is unpopulated or cold.</li>
 * </ol>
 */
@Component
@Primary
public class RedisDeliveryCandidateSelector implements DeliveryCandidateSelector {

    private static final Logger log = LoggerFactory.getLogger(RedisDeliveryCandidateSelector.class);

    private final DeliveryTrackingService deliveryTrackingService;
    private final PostgisDeliveryCandidateSelector postgisCandidateSelector;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final RoutingGateway routingGateway;
    private final DispatchProperties dispatchProperties;
    private final DeliveryGeoProperties deliveryGeoProperties;

    public RedisDeliveryCandidateSelector(
            DeliveryTrackingService deliveryTrackingService,
            PostgisDeliveryCandidateSelector postgisCandidateSelector,
            DeliveryAgentRepository deliveryAgentRepository,
            RoutingGateway routingGateway,
            DispatchProperties dispatchProperties,
            DeliveryGeoProperties deliveryGeoProperties
    ) {
        this.deliveryTrackingService = deliveryTrackingService;
        this.postgisCandidateSelector = postgisCandidateSelector;
        this.deliveryAgentRepository = deliveryAgentRepository;
        this.routingGateway = routingGateway;
        this.dispatchProperties = dispatchProperties;
        this.deliveryGeoProperties = deliveryGeoProperties;
    }

    @Override
    public Optional<DeliveryAgent> selectNextCandidate(Order order, double radiusKm, Set<UUID> excludedAgentIds) {
        if (!deliveryGeoProperties.enabled()) {
            return postgisCandidateSelector.selectNextCandidate(order, radiusKm, excludedAgentIds);
        }

        Point refPoint = extractReferencePoint(order);
        if (refPoint == null) {
            log.warn("Cannot select candidate: Order {} has no restaurant pickup location", order.getId());
            return Optional.empty();
        }

        double refLat = refPoint.getY();
        double refLng = refPoint.getX();

        // Query Redis GEO for nearby candidate drivers
        int queryLimit = Math.max(dispatchProperties.candidateLimit() * 2, 20);
        List<DriverGeoResult> geoCandidates = deliveryTrackingService.findNearbyAgents(refLat, refLng, radiusKm, queryLimit);

        List<DriverGeoResult> activeCandidates = geoCandidates.stream()
                .filter(c -> excludedAgentIds == null || !excludedAgentIds.contains(c.agentId()))
                .filter(c -> deliveryTrackingService.isAgentAlive(c.agentId()))
                .toList();

        if (activeCandidates.isEmpty()) {
            log.debug("No active candidate drivers found in Redis GEO within {} km for order {}, falling back to PostGIS",
                    radiusKm, order.getId());
            return postgisCandidateSelector.selectNextCandidate(order, radiusKm, excludedAgentIds);
        }

        // Hydrate and verify state against PostgreSQL
        List<UUID> candidateIds = activeCandidates.stream().map(DriverGeoResult::agentId).toList();
        List<DeliveryAgent> agents = deliveryAgentRepository.findAllById(candidateIds);
        Map<UUID, DeliveryAgent> agentMap = agents.stream().collect(Collectors.toMap(DeliveryAgent::getId, a -> a));

        List<EligibleCandidate> eligibleCandidates = new ArrayList<>();
        for (DriverGeoResult geoResult : activeCandidates) {
            DeliveryAgent agent = agentMap.get(geoResult.agentId());
            if (agent != null
                    && agent.isAvailable()
                    && !agent.isAssigned()
                    && agent.getCurrentStatus() == DeliveryAgentVerificationStatus.APPROVED
                    && agent.getCurrentVehicle() != null) {
                eligibleCandidates.add(new EligibleCandidate(agent, geoResult));
            }
        }

        if (eligibleCandidates.isEmpty()) {
            log.debug("All Redis candidates for order {} were ineligible in DB, falling back to PostGIS", order.getId());
            return postgisCandidateSelector.selectNextCandidate(order, radiusKm, excludedAgentIds);
        }

        if (eligibleCandidates.size() == 1) {
            return Optional.of(eligibleCandidates.getFirst().agent());
        }

        // Tier 2: Evaluate road driving durations via RoutingGateway
        List<GeoPoint> candidateLocations = eligibleCandidates.stream()
                .map(c -> {
                    if (c.geoResult().coordinate() != null) {
                        return GeoPoint.of(c.geoResult().coordinate().latitude(), c.geoResult().coordinate().longitude());
                    }
                    return GeoPoint.of(c.agent().getLastLocation().getY(), c.agent().getLastLocation().getX());
                })
                .toList();

        GeoPoint refGeo = GeoPoint.of(refLat, refLng);
        List<Long> durations = routingGateway.travelTimes(refGeo, candidateLocations);

        List<CandidateEvaluation> evaluations = new ArrayList<>(eligibleCandidates.size());
        for (int i = 0; i < eligibleCandidates.size(); i++) {
            long duration = (i < durations.size() && durations.get(i) != null)
                    ? durations.get(i)
                    : Long.MAX_VALUE;
            evaluations.add(new CandidateEvaluation(eligibleCandidates.get(i).agent(), duration));
        }

        // Sort by road driving duration ASC, then fairness tiebreaker (lastAssignedAt ASC, nulls first)
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

    private record EligibleCandidate(
            DeliveryAgent agent,
            DriverGeoResult geoResult
    ) {}

    private record CandidateEvaluation(
            DeliveryAgent agent,
            long durationSeconds
    ) {}
}
