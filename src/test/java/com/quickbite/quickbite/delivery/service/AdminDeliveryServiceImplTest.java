package com.quickbite.quickbite.delivery.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.quickbite.delivery.exception.DeliveryAgentNotFoundException;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.model.DeliveryAgentVerificationStatus;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDeliveryServiceImplTest {

    @Mock
    private DeliveryAgentRepository deliveryAgentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminDeliveryServiceImpl adminDeliveryService;

    private User admin;
    private User driverUser;
    private DeliveryAgent agent;
    private UUID adminId;
    private UUID agentId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        agentId = UUID.randomUUID();

        admin = new User();
        admin.setId(adminId);
        admin.setName("Admin Sarah");

        driverUser = new User();
        driverUser.setId(UUID.randomUUID());
        driverUser.setName("Driver Dave");
        driverUser.setEmail("dave@delivery.com");
        driverUser.setPhoneNumber("9876543210");

        agent = new DeliveryAgent();
        agent.setId(agentId);
        agent.setUser(driverUser);
        agent.setAvailable(true);
        agent.setAssigned(false);
        agent.setCurrentStatus(DeliveryAgentVerificationStatus.APPROVED);
        agent.setCreatedAt(Instant.now());
    }

    @Nested
    @DisplayName("suspendAgent")
    class SuspendAgentTests {

        @Test
        @DisplayName("Successfully suspends active agent and sets available to false")
        void suspendAgent_success() {
            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
            when(deliveryAgentRepository.findById(agentId)).thenReturn(Optional.of(agent));
            when(deliveryAgentRepository.save(any(DeliveryAgent.class))).thenAnswer(i -> i.getArgument(0));

            DeliveryAgentResponse res = adminDeliveryService.suspendAgent(agentId, adminId, "Reckless driving reports");

            assertThat(res.currentStatus()).isEqualTo(DeliveryAgentVerificationStatus.SUSPENDED);
            assertThat(agent.getCurrentStatus()).isEqualTo(DeliveryAgentVerificationStatus.SUSPENDED);
            assertThat(agent.isAvailable()).isFalse();
            verify(deliveryAgentRepository).save(agent);
        }

        @Test
        @DisplayName("Throws BadRequestException if agent is already suspended")
        void suspendAgent_alreadySuspended() {
            agent.setCurrentStatus(DeliveryAgentVerificationStatus.SUSPENDED);
            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
            when(deliveryAgentRepository.findById(agentId)).thenReturn(Optional.of(agent));

            assertThatThrownBy(() -> adminDeliveryService.suspendAgent(agentId, adminId, "Reason"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already suspended");

            verify(deliveryAgentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws DeliveryAgentNotFoundException when agent does not exist")
        void suspendAgent_agentNotFound() {
            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
            when(deliveryAgentRepository.findById(agentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminDeliveryService.suspendAgent(agentId, adminId, "Reason"))
                    .isInstanceOf(DeliveryAgentNotFoundException.class);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when admin does not exist")
        void suspendAgent_adminNotFound() {
            when(userRepository.findById(adminId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminDeliveryService.suspendAgent(agentId, adminId, "Reason"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("reinstateAgent")
    class ReinstateAgentTests {

        @Test
        @DisplayName("Successfully reinstates suspended agent back to APPROVED")
        void reinstateAgent_success() {
            agent.setCurrentStatus(DeliveryAgentVerificationStatus.SUSPENDED);
            agent.setAvailable(false);

            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
            when(deliveryAgentRepository.findById(agentId)).thenReturn(Optional.of(agent));
            when(deliveryAgentRepository.save(any(DeliveryAgent.class))).thenAnswer(i -> i.getArgument(0));

            DeliveryAgentResponse res = adminDeliveryService.reinstateAgent(agentId, adminId);

            assertThat(res.currentStatus()).isEqualTo(DeliveryAgentVerificationStatus.APPROVED);
            assertThat(agent.getCurrentStatus()).isEqualTo(DeliveryAgentVerificationStatus.APPROVED);
            verify(deliveryAgentRepository).save(agent);
        }

        @Test
        @DisplayName("Throws BadRequestException if agent is not currently suspended")
        void reinstateAgent_notSuspended() {
            agent.setCurrentStatus(DeliveryAgentVerificationStatus.APPROVED);
            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
            when(deliveryAgentRepository.findById(agentId)).thenReturn(Optional.of(agent));

            assertThatThrownBy(() -> adminDeliveryService.reinstateAgent(agentId, adminId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Only suspended agents can be reinstated");

            verify(deliveryAgentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws DeliveryAgentNotFoundException when agent does not exist")
        void reinstateAgent_agentNotFound() {
            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
            when(deliveryAgentRepository.findById(agentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminDeliveryService.reinstateAgent(agentId, adminId))
                    .isInstanceOf(DeliveryAgentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listAgentsByStatus")
    class ListAgentsTests {

        @Test
        @DisplayName("Returns paginated agents by status")
        void listAgentsByStatus_success() {
            when(deliveryAgentRepository.findWithCursor(eq(DeliveryAgentVerificationStatus.APPROVED), isNull(), eq(Limit.of(21))))
                    .thenReturn(List.of(agent));

            CursorPage<DeliveryAgentResponse> page = adminDeliveryService.listAgentsByStatus(
                    DeliveryAgentVerificationStatus.APPROVED, null, 20);

            assertThat(page.content()).hasSize(1);
            assertThat(page.content().get(0).id()).isEqualTo(agentId);
            assertThat(page.hasMore()).isFalse();
        }
    }
}
