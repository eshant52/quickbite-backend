package com.quickbite.quickbite.delivery.service;

import java.time.Instant;

/**
 * Result of checking pending offer status during dispatch processing.
 */
public record PendingOfferCheckResult(
        State state,
        Instant expiresAt
) {
    public enum State {
        ACTIVE,
        EXPIRED,
        NO_PENDING_OFFER
    }

    public static PendingOfferCheckResult active(Instant expiresAt) {
        return new PendingOfferCheckResult(State.ACTIVE, expiresAt);
    }

    public static PendingOfferCheckResult expired() {
        return new PendingOfferCheckResult(State.EXPIRED, null);
    }

    public static PendingOfferCheckResult noPendingOffer() {
        return new PendingOfferCheckResult(State.NO_PENDING_OFFER, null);
    }

    public boolean isActive() {
        return state == State.ACTIVE;
    }
}
