package com.quickbite.quickbite.delivery.service;

import com.quickbite.quickbite.order.model.Order;

/**
 * Port for automated delivery agent dispatch and assignment.
 *
 * @deprecated Superseded by the asynchronous offer-based dispatch flow via {@link DeliveryDispatchService#initiateDispatch(java.util.UUID)}.
 */
@Deprecated(forRemoval = true)
public interface DeliveryAssignmentService {

    /**
     * @deprecated Use {@link DeliveryDispatchService#initiateDispatch(java.util.UUID)} instead.
     */
    @Deprecated(forRemoval = true)
    void autoAssign(Order order);
}
