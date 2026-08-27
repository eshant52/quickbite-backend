package com.quickbite.quickbite.allotment.service.strategy;

import com.quickbite.quickbite.allotment.model.AllotmentReferenceType;
import com.quickbite.quickbite.allotment.repository.AdminAllotmentRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.UserRole;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Workload-balanced admin selection strategy.
 * <p>
 * Evaluates the current number of PENDING review tasks for each administrator and selects
 * the least-loaded admins first, ensuring proportional work distribution across the admin team.
 */
@Component
public class WorkloadBalancedAdminSelectionStrategy implements AdminSelectionStrategy {

    private final UserRepository userRepository;
    private final AdminAllotmentRepository allotmentRepository;

    public WorkloadBalancedAdminSelectionStrategy(
            UserRepository userRepository,
            AdminAllotmentRepository allotmentRepository) {
        this.userRepository = userRepository;
        this.allotmentRepository = allotmentRepository;
    }

    @Override
    public List<User> select(AllotmentReferenceType referenceType, int maxAssignees) {
        List<User> allAdmins = userRepository.findByRole(UserRole.ADMIN);
        if (allAdmins.isEmpty()) {
            return List.of();
        }

        Map<UUID, Long> pendingCountByAdmin = allotmentRepository
                .countPendingTasksPerAdmin()
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));

        return allAdmins.stream()
                .sorted(Comparator
                        .comparingLong((User a) -> pendingCountByAdmin.getOrDefault(a.getId(), 0L))
                        .thenComparing(User::getId))
                .limit(maxAssignees)
                .toList();
    }
}
