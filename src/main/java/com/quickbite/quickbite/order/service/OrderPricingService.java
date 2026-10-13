package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.cart.model.Cart;
import com.quickbite.quickbite.common.dto.PriceBreakdown;
import com.quickbite.quickbite.user.model.Address;

import java.math.BigDecimal;

/**
 * Encapsulates order and checkout pricing: routing, delivery fee chain,
 * platform fee, taxes, tips, and final totals.
 */
public interface OrderPricingService {

    /**
     * Calculates the full price breakdown for an active cart to be delivered to the specified address.
     *
     * @param cart            the customer's cart
     * @param customerAddress the target delivery address
     * @param tipAmount       optional tip amount
     * @return the calculated price breakdown
     */
    PriceBreakdown calculatePrice(Cart cart, Address customerAddress, BigDecimal tipAmount);
}
