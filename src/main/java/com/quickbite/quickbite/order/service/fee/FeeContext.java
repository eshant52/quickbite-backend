package com.quickbite.quickbite.order.service.fee;

import com.quickbite.quickbite.common.routing.GeoPoint;
import com.quickbite.quickbite.common.routing.RouteResult;

/**
 * Context object passed through the delivery fee calculation chain.
 * Populated before the chain runs; each calculator reads from it.
 */
public record FeeContext(
        GeoPoint restaurantLocation,
        GeoPoint customerLocation,
        RouteResult route
) {}
