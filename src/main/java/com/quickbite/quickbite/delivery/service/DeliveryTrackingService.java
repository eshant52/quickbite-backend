package com.quickbite.quickbite.delivery.service;

import com.quickbite.quickbite.delivery.dto.DriverGeoResult;
import com.quickbite.quickbite.delivery.dto.GeoCoordinate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing real-time geospatial driver positions and liveness heartbeats in Redis.
 */
public interface DeliveryTrackingService {

    /**
     * Records or updates a delivery agent's real-time GPS coordinate in Redis GEO
     * and refreshes their heartbeat liveness key.
     *
     * @param agentId   unique delivery agent ID
     * @param latitude  GPS latitude (-90 to 90)
     * @param longitude GPS longitude (-180 to 180)
     */
    void recordLocation(UUID agentId, double latitude, double longitude);

    /**
     * Fetches the latest known coordinate for a delivery agent from Redis.
     *
     * @param agentId unique delivery agent ID
     * @return Optional containing GeoCoordinate if present
     */
    Optional<GeoCoordinate> getAgentLocation(UUID agentId);

    /**
     * Queries Redis GEO for delivery agents within the specified radius around the given coordinates,
     * ordered from nearest to farthest.
     *
     * @param latitude  reference latitude (e.g. restaurant pickup)
     * @param longitude reference longitude
     * @param radiusKm  search radius in kilometers
     * @param limit     maximum candidates to return
     * @return list of candidate agents with distance and coordinates
     */
    List<DriverGeoResult> findNearbyAgents(double latitude, double longitude, double radiusKm, int limit);

    /**
     * Removes a delivery agent from the Redis GEO index and deletes their heartbeat key.
     * Used when an agent goes off-duty, finishes shift, or is suspended.
     *
     * @param agentId unique delivery agent ID
     */
    void removeAgent(UUID agentId);

    /**
     * Checks if a delivery agent currently has an active heartbeat in Redis.
     *
     * @param agentId unique delivery agent ID
     * @return true if heartbeat key is active in Redis
     */
    boolean isAgentAlive(UUID agentId);

    /**
     * Retrieves all currently active and alive driver coordinates from Redis GEO.
     * Used by the periodic write-behind sync scheduler.
     *
     * @return map of agentId to their latest GeoCoordinate
     */
    Map<UUID, GeoCoordinate> getAllActiveLocations();
}
