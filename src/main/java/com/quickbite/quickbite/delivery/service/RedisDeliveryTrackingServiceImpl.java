package com.quickbite.quickbite.delivery.service;

import com.quickbite.quickbite.common.config.property.DeliveryGeoProperties;
import com.quickbite.quickbite.delivery.dto.DriverGeoResult;
import com.quickbite.quickbite.delivery.dto.GeoCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoSearchCommandArgs;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RedisDeliveryTrackingServiceImpl implements DeliveryTrackingService {

    private static final Logger log = LoggerFactory.getLogger(RedisDeliveryTrackingServiceImpl.class);

    public static final String GEO_KEY = "quickbite:delivery:geo:locations";
    public static final String HEARTBEAT_PREFIX = "quickbite:delivery:geo:heartbeat:";

    private final StringRedisTemplate redisTemplate;
    private final DeliveryGeoProperties properties;

    public RedisDeliveryTrackingServiceImpl(
            StringRedisTemplate redisTemplate,
            DeliveryGeoProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public void recordLocation(UUID agentId, double latitude, double longitude) {
        if (agentId == null) {
            return;
        }

        // Note: Spring Data Geo Point accepts (x = longitude, y = latitude)
        Point point = new Point(longitude, latitude);
        String member = agentId.toString();

        redisTemplate.opsForGeo().add(GEO_KEY, point, member);
        redisTemplate.opsForValue().set(HEARTBEAT_PREFIX + member, "1", properties.heartbeatTtl());

        log.trace("Recorded driver {} position [lat={}, lng={}] with TTL {}",
                agentId, latitude, longitude, properties.heartbeatTtl());
    }

    @Override
    public Optional<GeoCoordinate> getAgentLocation(UUID agentId) {
        if (agentId == null) {
            return Optional.empty();
        }

        List<Point> positions = redisTemplate.opsForGeo().position(GEO_KEY, agentId.toString());
        if (positions != null && !positions.isEmpty() && positions.getFirst() != null) {
            Point pt = positions.getFirst();
            return Optional.of(new GeoCoordinate(pt.getY(), pt.getX()));
        }
        return Optional.empty();
    }

    @Override
    public List<DriverGeoResult> findNearbyAgents(double latitude, double longitude, double radiusKm, int limit) {
        if (radiusKm <= 0 || limit <= 0) {
            return Collections.emptyList();
        }

        GeoReference<String> reference = GeoReference.fromCoordinate(longitude, latitude);
        Distance distance = new Distance(radiusKm, Metrics.KILOMETERS);
        GeoSearchCommandArgs args = GeoSearchCommandArgs.newGeoSearchArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending()
                .limit(limit);

        GeoResults<GeoLocation<String>> geoResults = redisTemplate.opsForGeo().search(GEO_KEY, reference, distance, args);
        if (geoResults == null || geoResults.getContent().isEmpty()) {
            return Collections.emptyList();
        }

        List<DriverGeoResult> results = new ArrayList<>(geoResults.getContent().size());
        for (GeoResult<GeoLocation<String>> result : geoResults) {
            GeoLocation<String> location = result.getContent();
            try {
                UUID agentId = UUID.fromString(location.getName());
                double distKm = result.getDistance() != null ? result.getDistance().getValue() : 0.0;
                Point pt = location.getPoint();
                GeoCoordinate coord = pt != null ? new GeoCoordinate(pt.getY(), pt.getX()) : null;
                results.add(new DriverGeoResult(agentId, distKm, coord));
            } catch (IllegalArgumentException e) {
                log.warn("Skipping invalid UUID member in geo index: {}", location.getName());
            }
        }
        return results;
    }

    @Override
    public void removeAgent(UUID agentId) {
        if (agentId == null) {
            return;
        }
        String member = agentId.toString();
        redisTemplate.opsForGeo().remove(GEO_KEY, member);
        redisTemplate.delete(HEARTBEAT_PREFIX + member);
        log.debug("Removed driver {} from Redis GEO tracking", agentId);
    }

    @Override
    public boolean isAgentAlive(UUID agentId) {
        if (agentId == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(HEARTBEAT_PREFIX + agentId));
    }

    @Override
    public Map<UUID, GeoCoordinate> getAllActiveLocations() {
        Set<String> members = redisTemplate.opsForZSet().range(GEO_KEY, 0, -1);
        if (members == null || members.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, GeoCoordinate> activeLocations = new HashMap<>();
        List<String> deadMembers = new ArrayList<>();

        for (String member : members) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(HEARTBEAT_PREFIX + member))) {
                List<Point> positions = redisTemplate.opsForGeo().position(GEO_KEY, member);
                if (positions != null && !positions.isEmpty() && positions.getFirst() != null) {
                    Point pt = positions.getFirst();
                    try {
                        activeLocations.put(UUID.fromString(member), new GeoCoordinate(pt.getY(), pt.getX()));
                    } catch (IllegalArgumentException ignored) {
                        deadMembers.add(member);
                    }
                }
            } else {
                deadMembers.add(member);
            }
        }

        if (!deadMembers.isEmpty()) {
            redisTemplate.opsForGeo().remove(GEO_KEY, deadMembers.toArray(new String[0]));
            log.debug("Purged {} expired/dead drivers from Redis GEO index", deadMembers.size());
        }

        return activeLocations;
    }
}
