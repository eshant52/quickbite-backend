package com.quickbite.quickbite.cart.service;

import com.quickbite.quickbite.cart.dto.CheckoutPreviewResponse;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service providing read-only checkout previews before order placement.
 */
public interface CartCheckoutService {

    /**
     * Previews the cart checkout with full item details and price breakdown for a selected address.
     *
     * @param customerId the authenticated customer's user ID
     * @param addressId  the delivery address ID
     * @param tipAmount  optional tip amount
     * @return the checkout preview response
     */
    CheckoutPreviewResponse getCheckoutPreview(UUID customerId, UUID addressId, BigDecimal tipAmount);
}
