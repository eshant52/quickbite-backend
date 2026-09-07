package com.quickbite.quickbite.common.routing.adapter;

import com.quickbite.quickbite.common.config.property.RoutingProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.quickbite.quickbite.common.routing.CompositeRoutingGateway;
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
import java.util.stream.IntStream;

/**
 * Routing adapter for the self-hosted OSRM (Open Source Routing Machine) service.
 *
 * <p>API used:
 * <ul>
 *   <li>Route:  {@code GET /route/v1/driving/{lng},{lat};{lng},{lat}?overview=false}</li>
 *   <li>Matrix: {@code GET /table/v1/driving/{coords}?sources=0&destinations=1;2;...}</li>
 * </ul>
 *
 * <p>Throws {@link RoutingProviderUnavailableException} on any HTTP or parse error;
 * the {@link CompositeRoutingGateway} will catch it and fall through to the next provider.
 */
@Component
@ConditionalOnProperty(name = "quickbite.routing.osrm-enabled", havingValue = "true", matchIfMissing = true)
public class OsrmRoutingAdapter extends AbstractRoutingAdapter {

    private final RestClient restClient;
    private final String baseUrl;

    public OsrmRoutingAdapter(RoutingProperties properties) {
        this.baseUrl = properties.osrmBaseUrl() != null ? properties.osrmBaseUrl() : "http://localhost:5000";
        this.restClient = restClient(this.baseUrl, properties.timeout());
    }

    @Override
    protected String providerName() {
        return "OSRM";
    }

    // ── Route ─────────────────────────────────────────────────────────────────

    @Override
    protected RouteResult doRoute(GeoPoint from, GeoPoint to) {
        String coords = coordString(from) + ";" + coordString(to);
        URI uri = URI.create(baseUrl + "/route/v1/driving/" + coords + "?overview=false");

        OsrmRouteResponse response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(OsrmRouteResponse.class);

        if (response == null || response.routes() == null || response.routes().isEmpty()) {
            throw new RoutingProviderUnavailableException("OSRM returned empty route response");
        }

        OsrmRoute best = response.routes().getFirst();
        return new RouteResult(best.distance(), Math.round(best.duration()));
    }

    // ── Travel-time matrix ────────────────────────────────────────────────────

    @Override
    protected List<Long> doTravelTimes(GeoPoint source, List<GeoPoint> destinations) {
        // Build coordinate string: source first, then each destination
        String coords = coordString(source) + ";" +
                destinations.stream().map(this::coordString).collect(Collectors.joining(";"));

        // Destination indices: 1, 2, 3, ... (source is index 0)
        String destIndices = IntStream.rangeClosed(1, destinations.size())
                .mapToObj(Integer::toString)
                .collect(Collectors.joining(";"));

        URI uri = URI.create(baseUrl + "/table/v1/driving/" + coords
                + "?sources=0&destinations=" + destIndices + "&annotations=duration");

        OsrmTableResponse response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(OsrmTableResponse.class);

        if (response == null || response.durations() == null || response.durations().isEmpty()) {
            throw new RoutingProviderUnavailableException("OSRM returned empty table response");
        }

        // durations[0] is the single source row: durations[0][i] = source → destination[i]
        List<Double> row = response.durations().getFirst();
        List<Long> result = new ArrayList<>(row.size());
        for (Double d : row) {
            result.add(d == null ? Long.MAX_VALUE : Math.round(d));
        }
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String coordString(GeoPoint p) {
        return p.lng() + "," + p.lat();   // OSRM uses lng,lat order
    }

    // ── OSRM response records ─────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmRouteResponse(List<OsrmRoute> routes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmRoute(double distance, double duration) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmTableResponse(List<List<Double>> durations) {}
}
