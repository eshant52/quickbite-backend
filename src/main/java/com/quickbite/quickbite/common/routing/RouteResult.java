package com.quickbite.quickbite.common.routing;

/**
 * Routing result for a single-leg journey between two points.
 *
 * @param distanceMeters  road-network distance in metres
 * @param durationSeconds estimated driving duration in seconds
 */
public record RouteResult(double distanceMeters, long durationSeconds) {

    /** Convenience: distance in kilometres (2 decimal places). */
    public double distanceKm() {
        return Math.round((distanceMeters / 1000.0) * 100.0) / 100.0;
    }
}
