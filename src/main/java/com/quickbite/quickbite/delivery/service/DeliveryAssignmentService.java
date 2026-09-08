package com.quickbite.quickbite.delivery.service;

import com.quickbite.quickbite.order.model.Order;

/**
 * Port for automated delivery agent dispatch and assignment.
 */
public interface DeliveryAssignmentService {

    void autoAssign(Order order);
}
