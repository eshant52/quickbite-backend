package com.quickbite.quickbite.common.routing;

/**
 * Immutable geographic coordinate value object (WGS-84).
 *
 * @param lat latitude  (Y axis, −90 to +90)
 * @param lng longitude (X axis, −180 to +180)
 */
public record GeoPoint(double lat, double lng) {

    public static GeoPoint of(double lat, double lng) {
        return new GeoPoint(lat, lng);
    }
}
