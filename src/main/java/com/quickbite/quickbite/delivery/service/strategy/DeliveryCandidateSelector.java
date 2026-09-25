package com.quickbite.quickbite.delivery.service.strategy;

import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.order.model.Order;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Side-effect free candidate selection strategy interface for proactive delivery dispatch.
 */
public interface DeliveryCandidateSelector {

    /**
     * Selects the next eligible available delivery agent within the specified radius,
     * excluding agents who have already been offered this order.
     *
     * @param order             the order requiring dispatch
     * @param radiusKm          the search radius in kilometers
     * @param excludedAgentIds  set of delivery agent IDs that have already received offers
     * @return the best candidate agent, or empty if none available
     */
    Optional<DeliveryAgent> selectNextCandidate(Order order, double radiusKm, Set<UUID> excludedAgentIds);
}
