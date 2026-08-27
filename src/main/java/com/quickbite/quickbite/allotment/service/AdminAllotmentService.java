package com.quickbite.quickbite.allotment.service;

import com.quickbite.quickbite.allotment.model.AdminAllotment;
import com.quickbite.quickbite.allotment.model.AllotmentReferenceType;

import java.util.List;
import java.util.UUID;

public interface AdminAllotmentService {

    /**
     * Selects admins via strategy, persists PENDING allotment rows, and registers
     * an {@code AdminAllottedEvent} published after commit.
     *
     * @param referenceId the ID of the entity requiring review
     * @param referenceType the type of the review entity
     * @return the list of created allotment rows
     */
    List<AdminAllotment> allot(UUID referenceId, AllotmentReferenceType referenceType);

    /**
     * Accepts a pending allotment for an admin.
     * Marks this allotment as ACCEPTED and atomically declines all other PENDING
     * allotments for the same referenceId.
     *
     * @param allotmentId ID of the allotment row
     * @param adminId ID of the admin claiming the review
     * @return the updated AdminAllotment
     */
    AdminAllotment accept(UUID allotmentId, UUID adminId);

    /**
     * Explicitly declines a pending allotment for an admin.
     *
     * @param allotmentId ID of the allotment row
     * @param adminId ID of the admin declining the review
     */
    void decline(UUID allotmentId, UUID adminId);

    /**
     * Retrieves all PENDING allotments for a reference ID.
     */
    List<AdminAllotment> getPendingAllotments(UUID referenceId);

    /**
     * Retrieves all PENDING allotments assigned to a specific admin.
     */
    List<AdminAllotment> getMyPendingAllotments(UUID adminId);
}
