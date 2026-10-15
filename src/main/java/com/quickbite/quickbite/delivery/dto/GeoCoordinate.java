package com.quickbite.quickbite.delivery.dto;

/**
 * Immutable value representation of geographical coordinates (WGS-84).
 */
public record GeoCoordinate(double latitude, double longitude) {

    public static GeoCoordinate of(double latitude, double longitude) {
        return new GeoCoordinate(latitude, longitude);
    }
}
