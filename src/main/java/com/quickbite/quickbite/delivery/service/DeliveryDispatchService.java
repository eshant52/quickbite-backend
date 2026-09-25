package com.quickbite.quickbite.delivery.service;

import com.quickbite.quickbite.delivery.dto.OrderOfferSummaryResponse;

import java.util.UUID;

/**
 * Service orchestrating proactive delivery agent dispatch, offer lifecycles, and concurrency locks.
 */
public interface DeliveryDispatchService {

    /**
     * Initiates dispatch for an accepted order. Sets status to FINDING_AGENT and offers
     * to the best candidate in Round 0.
     *
     * @param orderId the order ID
     */
    void initiateDispatch(UUID orderId);

    /**
     * Processes due dispatch progression for an order whose current offer or round timer has expired.
     *
     * @param orderId the order ID
     */
    void processDueDispatch(UUID orderId);

    /**
     * Handles an agent's acceptance of an offered order with multi-layer concurrency defense.
     *
     * @param orderId     the order ID
     * @param agentUserId the user ID of the accepting agent
     */
    void acceptOffer(UUID orderId, UUID agentUserId);

    /**
     * Handles an agent declining an offer. Immediately falls back to the next candidate
     * in the same radius before escalating.
     *
     * @param orderId     the order ID
     * @param agentUserId the user ID of the rejecting agent
     */
    void rejectOffer(UUID orderId, UUID agentUserId);

    /**
     * Returns the offer summary (earnings, route, expiration timer) for a delivery agent.
     *
     * @param orderId     the order ID
     * @param agentUserId the user ID of the requesting agent
     * @return summary response
     */
    OrderOfferSummaryResponse getOfferSummary(UUID orderId, UUID agentUserId);
}
