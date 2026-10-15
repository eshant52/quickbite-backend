package com.quickbite.quickbite.delivery.scheduler;

import com.quickbite.quickbite.common.config.property.DeliveryGeoProperties;
import com.quickbite.quickbite.delivery.dto.GeoCoordinate;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.delivery.service.DeliveryTrackingService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Write-behind persistence worker for delivery driver locations.
 * Periodically syncs active driver coordinates from Redis GEO to PostgreSQL,
 * collapsing high-frequency per-second row churn into a single debounced batch write.
 */
@Component
public class DeliveryLocationSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeliveryLocationSyncScheduler.class);
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final DeliveryTrackingService deliveryTrackingService;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final DeliveryGeoProperties deliveryGeoProperties;

    public DeliveryLocationSyncScheduler(
            DeliveryTrackingService deliveryTrackingService,
            DeliveryAgentRepository deliveryAgentRepository,
            DeliveryGeoProperties deliveryGeoProperties
    ) {
        this.deliveryTrackingService = deliveryTrackingService;
        this.deliveryAgentRepository = deliveryAgentRepository;
        this.deliveryGeoProperties = deliveryGeoProperties;
    }

    @Scheduled(fixedDelayString = "${quickbite.delivery.geo.sync-interval-ms:30000}")
    @Transactional
    public void syncLocationsToDatabase() {
        if (!deliveryGeoProperties.enabled()) {
            return;
        }

        Map<UUID, GeoCoordinate> activeLocations = deliveryTrackingService.getAllActiveLocations();
        if (activeLocations.isEmpty()) {
            return;
        }

        List<DeliveryAgent> agents = deliveryAgentRepository.findAllById(activeLocations.keySet());
        List<DeliveryAgent> updated = new ArrayList<>(agents.size());

        for (DeliveryAgent agent : agents) {
            GeoCoordinate coord = activeLocations.get(agent.getId());
            if (coord != null) {
                agent.setLastLocation(GEOMETRY_FACTORY.createPoint(new Coordinate(coord.longitude(), coord.latitude())));
                updated.add(agent);
            }
        }

        if (!updated.isEmpty()) {
            deliveryAgentRepository.saveAll(updated);
            log.debug("Synchronized {} active delivery agent coordinates from Redis GEO to PostgreSQL", updated.size());
        }
    }
}
