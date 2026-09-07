package com.quickbite.quickbite.common.routing;

import com.quickbite.quickbite.common.routing.adapter.HaversineFallbackAdapter;
import com.quickbite.quickbite.common.routing.adapter.MapboxRoutingAdapter;
import com.quickbite.quickbite.common.routing.adapter.OsrmRoutingAdapter;
import com.quickbite.quickbite.common.routing.exception.RoutingProviderUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompositeRoutingGatewayTest {

    @Mock
    private OsrmRoutingAdapter osrm;

    @Mock
    private MapboxRoutingAdapter mapbox;

    private HaversineFallbackAdapter haversine;
    private CompositeRoutingGateway gateway;

    private final GeoPoint from = GeoPoint.of(12.9716, 77.5946);
    private final GeoPoint to = GeoPoint.of(12.9352, 77.6245);

    @BeforeEach
    void setUp() {
        haversine = new HaversineFallbackAdapter();
    }

    @Test
    @DisplayName("Returns OSRM result when OSRM succeeds (first in chain)")
    void route_osrmSuccess() {
        gateway = gateway(List.of(osrm, mapbox, haversine));
        RouteResult expected = new RouteResult(4500.0, 720);
        when(osrm.route(from, to)).thenReturn(expected);

        RouteResult result = gateway.route(from, to);

        assertThat(result).isEqualTo(expected);
        verify(osrm).route(from, to);
        verifyNoInteractions(mapbox);
    }

    @Test
    @DisplayName("Falls back to Mapbox when OSRM fails")
    void route_osrmFails_mapboxSucceeds() {
        gateway = gateway(List.of(osrm, mapbox, haversine));
        when(osrm.route(from, to)).thenThrow(new RoutingProviderUnavailableException("OSRM down"));
        RouteResult mapboxResult = new RouteResult(4600.0, 750);
        when(mapbox.route(from, to)).thenReturn(mapboxResult);

        RouteResult result = gateway.route(from, to);

        assertThat(result).isEqualTo(mapboxResult);
        verify(osrm).route(from, to);
        verify(mapbox).route(from, to);
    }

    @Test
    @DisplayName("Falls back to Haversine when both OSRM and Mapbox fail")
    void route_bothFail_haversineFallback() {
        gateway = gateway(List.of(osrm, mapbox, haversine));
        when(osrm.route(from, to)).thenThrow(new RoutingProviderUnavailableException("OSRM down"));
        when(mapbox.route(from, to)).thenThrow(new RoutingProviderUnavailableException("Mapbox down"));

        RouteResult result = gateway.route(from, to);

        assertThat(result).isNotNull();
        assertThat(result.distanceMeters()).isGreaterThan(0);
        assertThat(result.durationSeconds()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Single Haversine-only chain always succeeds")
    void route_haversineOnlyChain_alwaysSucceeds() {
        gateway = gateway(List.of(haversine));

        RouteResult result = gateway.route(from, to);

        assertThat(result).isNotNull();
        assertThat(result.distanceMeters()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Prod chain: Mapbox primary with Haversine fallback (no OSRM in chain)")
    void route_prodChain_mapboxWithHaversineFallback() {
        gateway = gateway(List.of(mapbox, haversine));
        when(mapbox.route(from, to)).thenThrow(new RoutingProviderUnavailableException("Mapbox rate limit"));

        RouteResult result = gateway.route(from, to);

        assertThat(result).isNotNull();
        assertThat(result.distanceMeters()).isGreaterThan(0);
        verify(mapbox).route(from, to);
        verifyNoInteractions(osrm);
    }

    @Test
    @DisplayName("Matrix travelTimes falls back to Haversine when OSRM and Mapbox fail")
    void travelTimes_fallback() {
        gateway = gateway(List.of(osrm, mapbox, haversine));
        List<GeoPoint> dests = List.of(to);
        when(osrm.travelTimes(from, dests)).thenThrow(new RoutingProviderUnavailableException("OSRM down"));
        when(mapbox.travelTimes(from, dests)).thenThrow(new RoutingProviderUnavailableException("Mapbox down"));

        List<Long> times = gateway.travelTimes(from, dests);

        assertThat(times).hasSize(1);
        assertThat(times.getFirst()).isGreaterThan(0);
    }

    private CompositeRoutingGateway gateway(List<RoutingGateway> providers) {
        return new CompositeRoutingGateway(new RoutingProviderChain(providers));
    }
}
