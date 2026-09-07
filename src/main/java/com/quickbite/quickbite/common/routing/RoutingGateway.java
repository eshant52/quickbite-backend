package com.quickbite.quickbite.common.routing;

import com.quickbite.quickbite.common.routing.exception.RoutingProviderUnavailableException;

import java.util.List;

/**
 * Domain-facing routing abstraction.
 *
 * <p>Service-layer code depends on this interface only — never on a
 * concrete adapter. The active implementation is resolved by Spring
 * (see {@link CompositeRoutingGateway}).
 */
public interface RoutingGateway {

    /**
     * Calculate the road-network route between two points.
     *
     * @param from origin coordinate
     * @param to   destination coordinate
     * @return distance (metres) + duration (seconds) for the fastest road route
     * @throws RoutingProviderUnavailableException if all providers fail
     */
    RouteResult route(GeoPoint from, GeoPoint to);

    /**
     * Calculate driving durations from one source to multiple destinations
     * (the OSRM / Mapbox "Matrix" or "Table" API).
     *
     * <p>Returns durations in seconds in the same order as {@code destinations}.
     * If a leg cannot be routed, {@code Long.MAX_VALUE} is returned for that index.
     *
     * @param source       the single origin (e.g. restaurant location)
     * @param destinations list of destinations (e.g. candidate driver locations)
     * @return list of driving durations in seconds, same size as {@code destinations}
     * @throws RoutingProviderUnavailableException if all providers fail
     */
    List<Long> travelTimes(GeoPoint source, List<GeoPoint> destinations);
}
