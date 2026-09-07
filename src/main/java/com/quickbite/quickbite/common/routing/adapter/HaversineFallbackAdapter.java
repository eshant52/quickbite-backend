package com.quickbite.quickbite.common.routing.adapter;

import com.quickbite.quickbite.common.routing.GeoPoint;
import com.quickbite.quickbite.common.routing.RouteResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Last-resort routing adapter using the Haversine formula.
 *
 * <p>Returns straight-line (great-circle) distances and a naive speed estimate
 * for durations. No external HTTP calls — always available.
 *
 * <p>Accuracy trade-off: straight-line distance is typically 60-70% of actual
 * road distance in urban areas. This adapter is only used when both OSRM and
 * Mapbox are unreachable; the system degrades gracefully rather than failing.
 */
@Component
public class HaversineFallbackAdapter extends AbstractRoutingAdapter {

    /** Earth radius in metres (WGS-84 mean radius). */
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    /**
     * Assumed average urban driving speed for estimating duration.
     * 25 km/h = 6.944 m/s — conservative city speed accounting for traffic.
     */
    private static final double AVERAGE_SPEED_MS = 25_000.0 / 3600.0;

    /**
     * Road-to-straight-line multiplier.
     * Real roads are typically 1.3-1.4× the straight-line distance.
     */
    private static final double ROAD_FACTOR = 1.35;

    @Override
    protected String providerName() {
        return "Haversine-Fallback";
    }

    @Override
    protected RouteResult doRoute(GeoPoint from, GeoPoint to) {
        double straightLine = haversineMeters(from, to);
        double estimated    = straightLine * ROAD_FACTOR;
        long   duration     = Math.round(estimated / AVERAGE_SPEED_MS);
        return new RouteResult(estimated, duration);
    }

    @Override
    protected List<Long> doTravelTimes(GeoPoint source, List<GeoPoint> destinations) {
        List<Long> times = new ArrayList<>(destinations.size());
        for (GeoPoint dest : destinations) {
            double estimated = haversineMeters(source, dest) * ROAD_FACTOR;
            times.add(Math.round(estimated / AVERAGE_SPEED_MS));
        }
        return times;
    }

    // ── Haversine formula ────────────────────────────────────────────────────

    public static double haversineMeters(GeoPoint a, GeoPoint b) {
        double lat1 = Math.toRadians(a.lat());
        double lat2 = Math.toRadians(b.lat());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(b.lng() - a.lng());

        double sin2Lat = Math.sin(dLat / 2) * Math.sin(dLat / 2);
        double sin2Lng = Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double h = sin2Lat + Math.cos(lat1) * Math.cos(lat2) * sin2Lng;
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(h));
    }
}
