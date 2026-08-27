package com.quickbite.quickbite.allotment.service;

import com.quickbite.quickbite.allotment.model.AllotmentReferenceType;
import com.quickbite.quickbite.allotment.repository.AdminAllotmentRepository;
import com.quickbite.quickbite.allotment.service.strategy.WorkloadBalancedAdminSelectionStrategy;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.UserRole;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkloadBalancedAdminSelectionStrategy")
class WorkloadBalancedAdminSelectionStrategyTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminAllotmentRepository allotmentRepository;

    private WorkloadBalancedAdminSelectionStrategy strategy;

    private User adminA;
    private User adminB;
    private User adminC;

    @BeforeEach
    void setUp() {
        strategy = new WorkloadBalancedAdminSelectionStrategy(userRepository, allotmentRepository);

        adminA = new User();
        adminA.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        adminA.setName("Admin A");

        adminB = new User();
        adminB.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        adminB.setName("Admin B");

        adminC = new User();
        adminC.setId(UUID.fromString("00000000-0000-0000-0000-000000000003"));
        adminC.setName("Admin C");
    }

    @Test
    @DisplayName("selects least loaded admins first according to pending task count")
    void selectsLeastLoadedFirst() {
        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of(adminA, adminB, adminC));

        // Admin B has 5 tasks, Admin C has 2 tasks, Admin A has 0 tasks
        List<Object[]> pendingCounts = List.of(
                new Object[]{adminB.getId(), 5L},
                new Object[]{adminC.getId(), 2L}
        );
        when(allotmentRepository.countPendingTasksPerAdmin()).thenReturn(pendingCounts);

        List<User> selected = strategy.select(AllotmentReferenceType.CUISINE, 2);

        // Should pick Admin A (0 tasks) then Admin C (2 tasks)
        assertThat(selected).containsExactly(adminA, adminC);
    }

    @Test
    @DisplayName("returns all admins up to maxAssignees when admins <= maxAssignees")
    void returnsAllWhenFewerThanMax() {
        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of(adminA, adminB));
        when(allotmentRepository.countPendingTasksPerAdmin()).thenReturn(List.of());

        List<User> selected = strategy.select(AllotmentReferenceType.RESTAURANT_APPLICATION, 4);

        assertThat(selected).hasSize(2);
        assertThat(selected).contains(adminA, adminB);
    }

    @Test
    @DisplayName("returns empty list when no admins exist")
    void returnsEmptyWhenNoAdmins() {
        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of());

        List<User> selected = strategy.select(AllotmentReferenceType.CUISINE, 4);

        assertThat(selected).isEmpty();
    }
}
