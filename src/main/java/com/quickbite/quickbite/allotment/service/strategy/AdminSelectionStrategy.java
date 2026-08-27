package com.quickbite.quickbite.allotment.service.strategy;

import com.quickbite.quickbite.allotment.model.AllotmentReferenceType;
import com.quickbite.quickbite.user.model.User;

import java.util.List;

/**
 * Strategy contract for selecting a subset of admin users to assign for a review task.
 */
public interface AdminSelectionStrategy {

    /**
     * Selects up to {@code maxAssignees} administrators to be assigned for the review.
     *
     * @param referenceType the type of task (RESTAURANT_APPLICATION, CUISINE, etc.)
     * @param maxAssignees the maximum number of admins to allot
     * @return list of selected admin users (ordered by priority)
     */
    List<User> select(AllotmentReferenceType referenceType, int maxAssignees);
}
