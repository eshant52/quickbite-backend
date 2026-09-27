package com.quickbite.quickbite.payment.service;

import com.quickbite.quickbite.payment.dto.PaymentResult;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain service responsible for reconciling multiple payment attempts (both online gateway
 * attempts and COD attempts) for a given order, enforcing "First Success Wins" semantics,
 * and dispatching automatic refunds or cancellations for superseded duplicate attempts.
 */
public interface PaymentReconciliationService {

    /**
     * Reconciles all previous payment attempts for the given order in chronological order.
     *
     * <p>If a paid attempt is found, it is reconciled to {@code SUCCESS} and returned as the
     * winning {@link PaymentResult}. Any secondary paid attempts are automatically refunded
     * (if online or delivered COD) or cancelled (if pre-delivery COD).
     *
     * @param orderId the order's UUID
     * @return Optional containing the winning PaymentResult if any attempt succeeded, or empty
     */
    Optional<PaymentResult> reconcileAllPaymentAttempts(UUID orderId);
}
