package com.quickbite.quickbite.payment.service.strategy;

import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.model.PaymentMethod;

public interface PaymentStrategy {
    /**
     * Initiates payment for the given order using this strategy.
     * Creates the Payment entity, status history, and transitions the order
     * to the appropriate status (PLACED for COD, AWAITING_PAYMENT for online).
     *
     * @param order         the saved order (must already be persisted)
     * @param paymentMethod the concrete payment method chosen by the customer
     * @return the payment result containing gateway-specific fields for the client
     */
    PaymentResult initiate(Order order, PaymentMethod paymentMethod);

    /** Returns true if this strategy handles the given payment method. */
    boolean supports(PaymentMethod paymentMethod);
}
