package com.quickbite.quickbite.delivery.service.strategy;

import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.order.model.Order;

import java.util.Optional;

public interface DeliveryAssignmentStrategy {
    Optional<DeliveryAgent> findAgent(Order order);
    String strategyName();
}
