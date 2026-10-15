package com.quickbite.quickbite.delivery.dto;

import java.util.UUID;

/**
 * Result representing a candidate driver located via geospatial search.
 */
public record DriverGeoResult(
        UUID agentId,
        double distanceKm,
        GeoCoordinate coordinate
) {}
