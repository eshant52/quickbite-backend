package com.quickbite.quickbite.common.routing.adapter;

import com.quickbite.quickbite.common.config.property.RoutingProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.quickbite.quickbite.common.routing.GeoPoint;
import com.quickbite.quickbite.common.routing.RouteResult;
import com.quickbite.quickbite.common.routing.exception.RoutingProviderUnavailableException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Routing adapter for the Mapbox Directions and Matrix APIs.
 *
 * <p>Only registered as a Spring bean when
 * {@code quickbite.routing.mapbox-enabled=true} is set in application properties.
 *
 * <p>APIs used:
 * <ul>
 *   <li>Route:  Mapbox Directions API v5</li>
 *   <li>Matrix: Mapbox Matrix API v1</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "quickbite.routing.mapbox-enabled", havingValue = "true")
public class MapboxRoutingAdapter extends AbstractRoutingAdapter {

    private static final String MAPBOX_DIRECTIONS_BASE = "https://api.mapbox.com";

    private final RestClient restClient;
    private final String accessToken;

    public MapboxRoutingAdapter(RoutingProperties properties) {
        this.restClient  = restClient(MAPBOX_DIRECTIONS_BASE, properties.timeout());
        this.accessToken = properties.mapboxAccessToken();
    }

    @Override
    protected String providerName() {
        return "Mapbox";
    }

    // ── Route ─────────────────────────────────────────────────────────────────

    @Override
    protected RouteResult doRoute(GeoPoint from, GeoPoint to) {
        // Mapbox: lng,lat order, semicolon-separated
        String coords = coordString(from) + ";" + coordString(to);
        URI uri = URI.create(MAPBOX_DIRECTIONS_BASE + "/directions/v5/mapbox/driving/" + coords
                + "?access_token=" + accessToken + "&overview=false");

        MapboxRouteResponse response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(MapboxRouteResponse.class);

        if (response == null || response.routes() == null || response.routes().isEmpty()) {
            throw new RoutingProviderUnavailableException("Mapbox returned empty route response");
        }

        MapboxRoute best = response.routes().getFirst();
        return new RouteResult(best.distance(), Math.round(best.duration()));
    }

    // ── Travel-time matrix ────────────────────────────────────────────────────

    @Override
    protected List<Long> doTravelTimes(GeoPoint source, List<GeoPoint> destinations) {
        // Mapbox Matrix: all coords as semicolons, sources and destinations as indices
        List<GeoPoint> all = new ArrayList<>();
        all.add(source);
        all.addAll(destinations);

        String coords = all.stream().map(this::coordString).collect(Collectors.joining(";"));
        // source index = 0; destination indices = 1..N
        String destIndices = java.util.stream.IntStream.rangeClosed(1, destinations.size())
                .mapToObj(Integer::toString)
                .collect(Collectors.joining(";"));

        URI uri = URI.create(MAPBOX_DIRECTIONS_BASE + "/directions-matrix/v1/mapbox/driving/" + coords
                + "?sources=0&destinations=" + destIndices + "&access_token=" + accessToken);

        MapboxTableResponse response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(MapboxTableResponse.class);

        if (response == null || response.durations() == null || response.durations().isEmpty()) {
            throw new RoutingProviderUnavailableException("Mapbox returned empty matrix response");
        }

        List<Double> row = response.durations().getFirst();
        List<Long> result = new ArrayList<>(row.size());
        for (Double d : row) {
            result.add(d == null ? Long.MAX_VALUE : Math.round(d));
        }
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String coordString(GeoPoint p) {
        return p.lng() + "," + p.lat();
    }

    // ── Mapbox response records ───────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MapboxRouteResponse(List<MapboxRoute> routes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MapboxRoute(double distance, double duration) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MapboxTableResponse(List<List<Double>> durations) {}
}
