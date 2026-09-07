package com.quickbite.quickbite.common.routing.adapter;

import com.quickbite.quickbite.common.routing.GeoPoint;
import com.quickbite.quickbite.common.routing.RouteResult;
import com.quickbite.quickbite.common.routing.RoutingGateway;
import com.quickbite.quickbite.common.routing.exception.RoutingProviderUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * Template Method base for routing adapters.
 *
 * <p>Provides shared logging and exception wrapping. Concrete adapters
 * override {@link #doRoute} and {@link #doTravelTimes} with their
 * provider-specific HTTP calls. They do NOT need to handle logging or
 * exception translation — that is done here.
 */
public abstract class AbstractRoutingAdapter implements RoutingGateway {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** Human-readable provider name used in logs and exception messages. */
    protected abstract String providerName();

    protected abstract RouteResult doRoute(GeoPoint from, GeoPoint to);

    protected abstract List<Long> doTravelTimes(GeoPoint source, List<GeoPoint> destinations);

    protected RestClient restClient(String baseUrl, Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public final RouteResult route(GeoPoint from, GeoPoint to) {
        log.debug("[{}] Routing from ({},{}) to ({},{})",
                providerName(), from.lat(), from.lng(), to.lat(), to.lng());
        try {
            RouteResult result = doRoute(from, to);
            log.debug("[{}] Route result: {}m / {}s", providerName(),
                    result.distanceMeters(), result.durationSeconds());
            return result;
        } catch (RoutingProviderUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            throw RoutingProviderUnavailableException.fromProvider(providerName(), ex);
        }
    }

    @Override
    public final List<Long> travelTimes(GeoPoint source, List<GeoPoint> destinations) {
        log.debug("[{}] Travel-time matrix: 1 source → {} destinations",
                providerName(), destinations.size());
        try {
            List<Long> times = doTravelTimes(source, destinations);
            log.debug("[{}] Travel-time matrix result: {}", providerName(), times);
            return times;
        } catch (RoutingProviderUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            throw RoutingProviderUnavailableException.fromProvider(providerName(), ex);
        }
    }
}
