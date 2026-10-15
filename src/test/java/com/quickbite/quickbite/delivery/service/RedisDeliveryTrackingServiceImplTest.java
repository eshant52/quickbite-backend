package com.quickbite.quickbite.delivery.service;

import com.quickbite.quickbite.common.config.property.DeliveryGeoProperties;
import com.quickbite.quickbite.delivery.dto.DriverGeoResult;
import com.quickbite.quickbite.delivery.dto.GeoCoordinate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoSearchCommandArgs;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisDeliveryTrackingServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private GeoOperations<String, String> geoOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private DeliveryGeoProperties properties;
    private RedisDeliveryTrackingServiceImpl trackingService;

    @BeforeEach
    void setUp() {
        properties = new DeliveryGeoProperties(true, Duration.ofSeconds(60), Duration.ofSeconds(30));
        trackingService = new RedisDeliveryTrackingServiceImpl(redisTemplate, properties);
    }

    @Test
    @DisplayName("recordLocation stores coordinate with GEOADD and sets heartbeat TTL")
    void recordLocation_success() {
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        UUID agentId = UUID.randomUUID();
        trackingService.recordLocation(agentId, 12.9716, 77.5946);

        verify(geoOperations).add(
                eq(RedisDeliveryTrackingServiceImpl.GEO_KEY),
                eq(new Point(77.5946, 12.9716)),
                eq(agentId.toString())
        );
        verify(valueOperations).set(
                eq(RedisDeliveryTrackingServiceImpl.HEARTBEAT_PREFIX + agentId),
                eq("1"),
                eq(Duration.ofSeconds(60))
        );
    }

    @Test
    @DisplayName("getAgentLocation returns coordinate when present in Redis")
    void getAgentLocation_found() {
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        UUID agentId = UUID.randomUUID();
        when(geoOperations.position(RedisDeliveryTrackingServiceImpl.GEO_KEY, agentId.toString()))
                .thenReturn(List.of(new Point(77.5946, 12.9716)));

        Optional<GeoCoordinate> coord = trackingService.getAgentLocation(agentId);

        assertThat(coord).isPresent();
        assertThat(coord.get().latitude()).isEqualTo(12.9716);
        assertThat(coord.get().longitude()).isEqualTo(77.5946);
    }

    @Test
    @DisplayName("getAgentLocation returns empty when not found")
    void getAgentLocation_notFound() {
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        UUID agentId = UUID.randomUUID();
        when(geoOperations.position(RedisDeliveryTrackingServiceImpl.GEO_KEY, agentId.toString()))
                .thenReturn(Collections.emptyList());

        Optional<GeoCoordinate> coord = trackingService.getAgentLocation(agentId);

        assertThat(coord).isEmpty();
    }

    @Test
    @DisplayName("findNearbyAgents parses Redis GEO search results properly")
    void findNearbyAgents_success() {
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        UUID agentId = UUID.randomUUID();

        GeoLocation<String> location = new GeoLocation<>(agentId.toString(), new Point(77.5950, 12.9720));
        GeoResult<GeoLocation<String>> geoResult = new GeoResult<>(location, new Distance(1.2, Metrics.KILOMETERS));
        GeoResults<GeoLocation<String>> results = new GeoResults<>(List.of(geoResult));

        when(geoOperations.search(
                eq(RedisDeliveryTrackingServiceImpl.GEO_KEY),
                any(GeoReference.class),
                any(Distance.class),
                any(GeoSearchCommandArgs.class)
        )).thenReturn(results);

        List<DriverGeoResult> candidates = trackingService.findNearbyAgents(12.9716, 77.5946, 5.0, 10);

        assertThat(candidates).hasSize(1);
        DriverGeoResult first = candidates.getFirst();
        assertThat(first.agentId()).isEqualTo(agentId);
        assertThat(first.distanceKm()).isEqualTo(1.2);
        assertThat(first.coordinate().latitude()).isEqualTo(12.9720);
        assertThat(first.coordinate().longitude()).isEqualTo(77.5950);
    }

    @Test
    @DisplayName("removeAgent purges driver from GEO and deletes heartbeat")
    void removeAgent_success() {
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        UUID agentId = UUID.randomUUID();

        trackingService.removeAgent(agentId);

        verify(geoOperations).remove(RedisDeliveryTrackingServiceImpl.GEO_KEY, agentId.toString());
        verify(redisTemplate).delete(RedisDeliveryTrackingServiceImpl.HEARTBEAT_PREFIX + agentId);
    }

    @Test
    @DisplayName("isAgentAlive checks heartbeat key existence")
    void isAgentAlive_success() {
        UUID agentId = UUID.randomUUID();
        when(redisTemplate.hasKey(RedisDeliveryTrackingServiceImpl.HEARTBEAT_PREFIX + agentId))
                .thenReturn(true);

        boolean alive = trackingService.isAgentAlive(agentId);

        assertThat(alive).isTrue();
    }

    @Test
    @DisplayName("getAllActiveLocations returns live agents and purges dead agents")
    void getAllActiveLocations_purgesDeadAgents() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);

        UUID liveAgentId = UUID.randomUUID();
        UUID deadAgentId = UUID.randomUUID();

        when(zSetOperations.range(RedisDeliveryTrackingServiceImpl.GEO_KEY, 0, -1))
                .thenReturn(Set.of(liveAgentId.toString(), deadAgentId.toString()));

        when(redisTemplate.hasKey(RedisDeliveryTrackingServiceImpl.HEARTBEAT_PREFIX + liveAgentId))
                .thenReturn(true);
        when(redisTemplate.hasKey(RedisDeliveryTrackingServiceImpl.HEARTBEAT_PREFIX + deadAgentId))
                .thenReturn(false);

        when(geoOperations.position(RedisDeliveryTrackingServiceImpl.GEO_KEY, liveAgentId.toString()))
                .thenReturn(List.of(new Point(77.5946, 12.9716)));

        Map<UUID, GeoCoordinate> activeLocations = trackingService.getAllActiveLocations();

        assertThat(activeLocations).hasSize(1);
        assertThat(activeLocations).containsKey(liveAgentId);
        assertThat(activeLocations.get(liveAgentId).latitude()).isEqualTo(12.9716);

        // Verify dead agent was purged from GEO set
        verify(geoOperations).remove(RedisDeliveryTrackingServiceImpl.GEO_KEY, new String[]{deadAgentId.toString()});
    }
}
