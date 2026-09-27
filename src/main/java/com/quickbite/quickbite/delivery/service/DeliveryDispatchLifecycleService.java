package com.quickbite.quickbite.delivery.service;

import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.model.DeliveryOffer;
import com.quickbite.quickbite.delivery.model.OrderDispatch;
import com.quickbite.quickbite.order.model.Order;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Domain service managing atomic lifecycle state transitions, persistence,
 * and event dispatching for order delivery dispatches and targeted driver offers.
 *
 * <p>All operations execute within fast, isolated database transactions,
 * ensuring database connections are never held open across external routing network calls.
 */
public interface DeliveryDispatchLifecycleService {

    /**
     * Initializes an OrderDispatch in FINDING_AGENT status within an atomic transaction.
     * Returns empty if dispatch was already initiated and not in NOT_STARTED status.
     */
    Optional<OrderDispatch> createInitialDispatch(UUID orderId, Duration totalDispatchWindow);

    /**
     * Checks if a pending offer exists. If expired, transitions it to EXPIRED and commits.
     * If still active, returns ACTIVE status with its expiresAt timestamp.
     */
    PendingOfferCheckResult checkAndExpirePendingOffer(UUID orderId);

    /**
     * Records an agent's rejection of a pending delivery offer in an atomic transaction.
     * Returns the current OrderDispatch if a pending offer was found and marked REJECTED,
     * or empty if no pending offer existed.
     */
    Optional<OrderDispatch> recordOfferRejection(UUID orderId, UUID agentUserId);

    /**
     * Atomically creates a PENDING DeliveryOffer, updates the OrderDispatch's currentRound
     * and nextAttemptAt, and publishes a DeliveryOfferCreatedEvent.
     */
    DeliveryOffer recordCreatedOffer(
            Order order,
            DeliveryAgent agent,
            int roundNumber,
            double radiusKm,
            Instant expiresAt
    );

    /**
     * Updates OrderDispatch with the next attempt timestamp and current round.
     */
    void recordRetryAttempt(UUID orderId, int currentRound, Instant nextAttemptAt);

    /**
     * Atomically transitions the OrderDispatch to EXHAUSTED and withdraws any pending offer.
     * Returns true if the order should subsequently be cancelled due to no delivery agent,
     * or false if the dispatch was already exhausted or the order is already in a terminal state.
     */
    boolean markDispatchExhausted(UUID orderId);

    /**
     * Atomically assigns the delivery agent to the order upon offer acceptance,
     * updates offer status to ACCEPTED, dispatch to AGENT_ASSIGNED, agent to assigned,
     * and publishes DeliveryAgentAssignedEvent.
     */
    void assignAgent(UUID orderId, UUID agentUserId);

    /**
     * Retrieves the set of delivery agent IDs that have already been offered this order.
     */
    Set<UUID> getOfferedAgentIds(UUID orderId);

    /**
     * Retrieves the current OrderDispatch for an order.
     */
    Optional<OrderDispatch> findDispatch(UUID orderId);

    /**
     * Returns true if an OrderDispatch exists for the order and is currently in FINDING_AGENT status.
     */
    boolean isDispatchActive(UUID orderId);

    /**
     * Resolves the DeliveryAgent profile for the given authenticated User ID.
     */
    DeliveryAgent findAgentForUser(UUID userId);
}
