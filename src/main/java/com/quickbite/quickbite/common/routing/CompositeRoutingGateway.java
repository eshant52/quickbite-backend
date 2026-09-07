package com.quickbite.quickbite.common.routing;

import com.quickbite.quickbite.common.routing.exception.RoutingProviderUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Chain-of-Responsibility composite routing gateway.
 *
 * <p>Iterates through an explicitly ordered provider chain (configured in
 * {@link com.quickbite.quickbite.common.config.RoutingConfig}) and returns the
 * result of the first provider that succeeds. If a provider throws
 * {@link RoutingProviderUnavailableException}, the next in line is tried.
 *
 * <p><strong>Profile chain order</strong>: OSRM → Haversine in development and
 * Mapbox → Haversine in production. Haversine is the terminal fallback — it is pure math and never throws — so
 * callers are guaranteed a result even when all HTTP providers are down.
 *
 * <p>This is the bean service-layer classes inject via {@link RoutingGateway}.
 * It depends on the abstract interface, not on any concrete adapter class,
 * satisfying the Dependency-Inversion Principle.
 */
@Primary
@Component
public class CompositeRoutingGateway implements RoutingGateway {

    private static final Logger log = LoggerFactory.getLogger(CompositeRoutingGateway.class);

    private final List<RoutingGateway> providers;

    /**
     * @param providerChain routing providers injected by
     *                      {@link com.quickbite.quickbite.common.config.RoutingConfig}
     */
    public CompositeRoutingGateway(RoutingProviderChain providerChain) {
        this.providers = providerChain.providers();
    }

    @Override
    public RouteResult route(GeoPoint from, GeoPoint to) {
        Exception lastCause = null;
        for (RoutingGateway provider : providers) {
            try {
                return provider.route(from, to);
            } catch (RoutingProviderUnavailableException ex) {
                log.warn("[{}] route unavailable, trying next provider. Reason: {}",
                        providerName(provider), ex.getMessage());
                lastCause = ex;
            }
        }
        // Should never be reached — HaversineFallbackAdapter is always last and never throws
        throw new RoutingProviderUnavailableException(
                "All routing providers exhausted with no successful route result", lastCause);
    }

    @Override
    public List<Long> travelTimes(GeoPoint source, List<GeoPoint> destinations) {
        Exception lastCause = null;
        for (RoutingGateway provider : providers) {
            try {
                return provider.travelTimes(source, destinations);
            } catch (RoutingProviderUnavailableException ex) {
                log.warn("[{}] travelTimes unavailable, trying next provider. Reason: {}",
                        providerName(provider), ex.getMessage());
                lastCause = ex;
            }
        }
        throw new RoutingProviderUnavailableException(
                "All routing providers exhausted with no successful travelTimes result", lastCause);
    }

    private String providerName(RoutingGateway gw) {
        return gw.getClass().getSimpleName();
    }
}
