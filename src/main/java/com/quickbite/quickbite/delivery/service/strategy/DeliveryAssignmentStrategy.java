package com.quickbite.quickbite.delivery.service.strategy;

import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.order.model.Order;

import java.util.Optional;

/**
 * @deprecated Superseded by {@link DeliveryCandidateSelector} in the asynchronous offer-based dispatch flow.
 */
@Deprecated(forRemoval = true)
public interface DeliveryAssignmentStrategy {
    Optional<DeliveryAgent> findAgent(Order order);
    String strategyName();
}
