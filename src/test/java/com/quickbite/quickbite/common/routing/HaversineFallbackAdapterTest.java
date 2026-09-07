package com.quickbite.quickbite.common.routing;

import com.quickbite.quickbite.common.routing.adapter.HaversineFallbackAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HaversineFallbackAdapterTest {

    private HaversineFallbackAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new HaversineFallbackAdapter();
    }

    @Test
    @DisplayName("Calculates approximate road distance between two points")
    void route_calculatesDistanceAndDuration() {
        // Point A: MG Road Bangalore (12.9754, 77.6066)
        // Point B: Indiranagar Bangalore (12.9784, 77.6408)
        // Straight line is ~3.7 km, road distance ~5.0 km
        GeoPoint from = GeoPoint.of(12.9754, 77.6066);
        GeoPoint to = GeoPoint.of(12.9784, 77.6408);

        RouteResult result = adapter.route(from, to);

        assertThat(result).isNotNull();
        assertThat(result.distanceKm()).isCloseTo(5.0, within(1.0));
        assertThat(result.durationSeconds()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Travel times matrix computes durations for all destinations")
    void travelTimes_computesDurations() {
        GeoPoint source = GeoPoint.of(12.9754, 77.6066);
        List<GeoPoint> destinations = List.of(
                GeoPoint.of(12.9784, 77.6408),
                GeoPoint.of(12.9352, 77.6245),
                GeoPoint.of(12.9754, 77.6066) // same spot
        );

        List<Long> times = adapter.travelTimes(source, destinations);

        assertThat(times).hasSize(3);
        assertThat(times.get(0)).isGreaterThan(0);
        assertThat(times.get(1)).isGreaterThan(0);
        assertThat(times.get(2)).isEqualTo(0);
    }
}
