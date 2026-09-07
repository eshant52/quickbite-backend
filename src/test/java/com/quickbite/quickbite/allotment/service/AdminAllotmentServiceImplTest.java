package com.quickbite.quickbite.allotment.service;

import com.quickbite.quickbite.allotment.exception.AllotmentAlreadyClaimedException;
import com.quickbite.quickbite.allotment.exception.AllotmentAlreadyExistsException;
import com.quickbite.quickbite.allotment.exception.AllotmentNotFoundException;
import com.quickbite.quickbite.allotment.model.AdminAllotment;
import com.quickbite.quickbite.allotment.model.AllotmentReferenceType;
import com.quickbite.quickbite.allotment.model.AllotmentStatus;
import com.quickbite.quickbite.allotment.repository.AdminAllotmentRepository;
import com.quickbite.quickbite.allotment.service.strategy.AdminSelectionStrategy;
import com.quickbite.quickbite.common.config.property.AllotmentProperties;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAllotmentServiceImpl")
class AdminAllotmentServiceImplTest {

    @Mock
    private AdminAllotmentRepository allotmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminSelectionStrategy selectionStrategy;

    private AllotmentProperties properties;
    private AdminAllotmentServiceImpl service;

    private UUID referenceId;
    private UUID admin1Id;
    private UUID admin2Id;
    private User admin1;
    private User admin2;

    @BeforeEach
    void setUp() {
        properties = new AllotmentProperties(4);
        service = new AdminAllotmentServiceImpl(
                allotmentRepository,
                userRepository,
                selectionStrategy,
                properties
        );

        referenceId = UUID.randomUUID();
        admin1Id = UUID.randomUUID();
        admin2Id = UUID.randomUUID();

        admin1 = new User();
        admin1.setId(admin1Id);
        admin1.setName("Admin One");

        admin2 = new User();
        admin2.setId(admin2Id);
        admin2.setName("Admin Two");
    }

    @Nested
    @DisplayName("allot")
    class AllotTests {

        @Test
        @DisplayName("successfully selects admins and saves allotments")
        void allotsSuccessfully() {
            when(allotmentRepository.existsByReferenceId(referenceId)).thenReturn(false);
            when(selectionStrategy.select(AllotmentReferenceType.CUISINE, 4)).thenReturn(List.of(admin1, admin2));
            when(allotmentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            List<AdminAllotment> result = service.allot(referenceId, AllotmentReferenceType.CUISINE);

            assertThat(result).hasSize(2);
            assertThat(result.getFirst().getAdmin()).isEqualTo(admin1);
            assertThat(result.getFirst().getStatus()).isEqualTo(AllotmentStatus.PENDING);
            assertThat(result.getFirst().getReferenceType()).isEqualTo(AllotmentReferenceType.CUISINE);
        }

        @Test
        @DisplayName("throws AllotmentAlreadyExistsException if referenceId already allotted")
        void throwsWhenAlreadyAllotted() {
            when(allotmentRepository.existsByReferenceId(referenceId)).thenReturn(true);

            assertThatThrownBy(() -> service.allot(referenceId, AllotmentReferenceType.CUISINE))
                    .isInstanceOf(AllotmentAlreadyExistsException.class)
                    .hasMessageContaining("already created");

            verify(selectionStrategy, never()).select(any(), anyInt());
        }

        @Test
        @DisplayName("returns empty list if no admins available")
        void returnsEmptyWhenNoAdmins() {
            when(allotmentRepository.existsByReferenceId(referenceId)).thenReturn(false);
            when(selectionStrategy.select(AllotmentReferenceType.RESTAURANT_APPLICATION, 4)).thenReturn(List.of());

            List<AdminAllotment> result = service.allot(referenceId, AllotmentReferenceType.RESTAURANT_APPLICATION);

            assertThat(result).isEmpty();
            verify(allotmentRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("accept")
    class AcceptTests {

        @Test
        @DisplayName("successfully marks allotment ACCEPTED and declines competitors")
        void acceptsSuccessfully() {
            UUID allotmentId = UUID.randomUUID();
            AdminAllotment allotment = new AdminAllotment();
            allotment.setId(allotmentId);
            allotment.setAdmin(admin1);
            allotment.setReferenceId(referenceId);
            allotment.setStatus(AllotmentStatus.PENDING);

            when(allotmentRepository.findById(allotmentId)).thenReturn(Optional.of(allotment));
            when(allotmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AdminAllotment accepted = service.accept(allotmentId, admin1Id);

            assertThat(accepted.getStatus()).isEqualTo(AllotmentStatus.ACCEPTED);
            assertThat(accepted.getRespondedAt()).isNotNull();

            verify(allotmentRepository).declineOthers(eq(referenceId), eq(allotmentId), any(Instant.class));
        }

        @Test
        @DisplayName("throws BadRequestException if admin does not own the allotment")
        void throwsWhenNotAssignedAdmin() {
            UUID allotmentId = UUID.randomUUID();
            AdminAllotment allotment = new AdminAllotment();
            allotment.setId(allotmentId);
            allotment.setAdmin(admin1);
            allotment.setStatus(AllotmentStatus.PENDING);

            when(allotmentRepository.findById(allotmentId)).thenReturn(Optional.of(allotment));

            assertThatThrownBy(() -> service.accept(allotmentId, admin2Id))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("not assigned to your account");
        }

        @Test
        @DisplayName("throws AllotmentAlreadyClaimedException if status is not PENDING")
        void throwsWhenAlreadyClaimed() {
            UUID allotmentId = UUID.randomUUID();
            AdminAllotment allotment = new AdminAllotment();
            allotment.setId(allotmentId);
            allotment.setAdmin(admin1);
            allotment.setStatus(AllotmentStatus.DECLINED);

            when(allotmentRepository.findById(allotmentId)).thenReturn(Optional.of(allotment));

            assertThatThrownBy(() -> service.accept(allotmentId, admin1Id))
                    .isInstanceOf(AllotmentAlreadyClaimedException.class)
                    .hasMessageContaining("already been claimed");
        }

        @Test
        @DisplayName("throws AllotmentNotFoundException if allotment ID does not exist")
        void throwsWhenNotFound() {
            UUID allotmentId = UUID.randomUUID();
            when(allotmentRepository.findById(allotmentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.accept(allotmentId, admin1Id))
                    .isInstanceOf(AllotmentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("decline")
    class DeclineTests {

        @Test
        @DisplayName("successfully marks allotment DECLINED")
        void declinesSuccessfully() {
            UUID allotmentId = UUID.randomUUID();
            AdminAllotment allotment = new AdminAllotment();
            allotment.setId(allotmentId);
            allotment.setAdmin(admin1);
            allotment.setStatus(AllotmentStatus.PENDING);

            when(allotmentRepository.findById(allotmentId)).thenReturn(Optional.of(allotment));

            service.decline(allotmentId, admin1Id);

            assertThat(allotment.getStatus()).isEqualTo(AllotmentStatus.DECLINED);
            assertThat(allotment.getRespondedAt()).isNotNull();
            verify(allotmentRepository).save(allotment);
        }
    }
}
