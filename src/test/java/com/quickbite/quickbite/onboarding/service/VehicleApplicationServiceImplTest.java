package com.quickbite.quickbite.onboarding.service;

import com.quickbite.quickbite.allotment.model.AllotmentReferenceType;
import com.quickbite.quickbite.allotment.service.AdminAllotmentService;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.onboarding.dto.vehicle.VehicleApplicationResponse;
import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.model.vehicle.VehicleApplication;
import com.quickbite.quickbite.onboarding.model.vehicle.VehicleApplicationDocument;
import com.quickbite.quickbite.onboarding.repository.DeliveryAgentApplicationRepository;
import com.quickbite.quickbite.onboarding.repository.DeliveryAgentApplicationVehicleDocumentRepository;
import com.quickbite.quickbite.onboarding.repository.DeliveryAgentApplicationVehicleRepository;
import com.quickbite.quickbite.onboarding.service.vehicle.VehicleApplicationServiceImpl;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import com.quickbite.quickbite.vehicle.model.Vehicle;
import com.quickbite.quickbite.vehicle.model.VehicleOwnership;
import com.quickbite.quickbite.vehicle.model.VehicleOwnershipDocumentType;
import com.quickbite.quickbite.vehicle.model.VehicleType;
import com.quickbite.quickbite.vehicle.repository.VehicleOwnershipDocumentRepository;
import com.quickbite.quickbite.vehicle.repository.VehicleOwnershipRepository;
import com.quickbite.quickbite.vehicle.repository.VehicleOwnershipStatusHistoryRepository;
import com.quickbite.quickbite.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
class VehicleApplicationServiceImplTest {

    @Mock private DeliveryAgentApplicationRepository deliveryAgentApplicationRepository;
    @Mock private DeliveryAgentApplicationVehicleRepository applicationVehicleRepository;
    @Mock private DeliveryAgentApplicationVehicleDocumentRepository applicationVehicleDocumentRepository;
    @Mock private DeliveryAgentRepository deliveryAgentRepository;
    @Mock private UserRepository userRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private VehicleOwnershipRepository vehicleOwnershipRepository;
    @Mock private VehicleOwnershipDocumentRepository vehicleOwnershipDocumentRepository;
    @Mock private VehicleOwnershipStatusHistoryRepository vehicleOwnershipStatusHistoryRepository;
    @Mock private AdminAllotmentService adminAllotmentService;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private VehicleApplicationServiceImpl vehicleApplicationService;

    private User agentUser;
    private User adminUser;
    private DeliveryAgent agent;
    private VehicleApplication vehicleApp;
    private UUID userId;
    private UUID adminId;
    private UUID agentId;
    private UUID vehicleAppId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        agentId = UUID.randomUUID();
        vehicleAppId = UUID.randomUUID();

        agentUser = new User();
        agentUser.setId(userId);
        agentUser.setName("Agent Bob");

        adminUser = new User();
        adminUser.setId(adminId);
        adminUser.setName("Admin Sarah");

        agent = new DeliveryAgent();
        agent.setId(agentId);
        agent.setUser(agentUser);

        vehicleApp = new VehicleApplication();
        vehicleApp.setId(vehicleAppId);
        vehicleApp.setDeliveryAgent(agent);
        vehicleApp.setVinNumber("VIN987654321");
        vehicleApp.setNumberPlate("MH-12-AB-9999");
        vehicleApp.setVehicleType(VehicleType.SCOOTER);
        vehicleApp.setBrand("Honda");
        vehicleApp.setModel("Activa 6G");
        vehicleApp.setStatus(ApplicationStatus.DRAFT);
        vehicleApp.setCreatedAt(Instant.now());
        vehicleApp.setUpdatedAt(Instant.now());
    }

    @Nested
    @DisplayName("startApplication (standalone)")
    class StartApplicationTests {

        @Test
        @DisplayName("Creates draft standalone vehicle application for approved driver")
        void startApplication_success() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(agentUser));
            when(deliveryAgentRepository.findByUser(agentUser)).thenReturn(Optional.of(agent));
            when(applicationVehicleRepository.save(any(VehicleApplication.class))).thenAnswer(i -> {
                VehicleApplication v = i.getArgument(0);
                v.setId(vehicleAppId);
                return v;
            });

            VehicleApplicationResponse res = vehicleApplicationService.startApplication(userId);

            assertThat(res.status()).isEqualTo(ApplicationStatus.DRAFT);
            verify(applicationVehicleRepository).save(any(VehicleApplication.class));
        }
    }

    @Nested
    @DisplayName("submitVehicleApplication")
    class SubmitTests {

        @Test
        @DisplayName("Submits standalone application and allots to admin when RC and Insurance are present")
        void submitVehicleApplication_success() {
            // Using the new dual-flow ownership lookup
            when(applicationVehicleRepository.findByIdAndOwnerUserId(vehicleAppId, userId))
                    .thenReturn(Optional.of(vehicleApp));
            when(applicationVehicleDocumentRepository.existsByApplicationVehicleAndType(
                    vehicleApp, VehicleOwnershipDocumentType.RC)).thenReturn(true);
            when(applicationVehicleDocumentRepository.existsByApplicationVehicleAndType(
                    vehicleApp, VehicleOwnershipDocumentType.INSURANCE)).thenReturn(true);
            when(applicationVehicleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            VehicleApplicationResponse res = vehicleApplicationService.submitVehicleApplication(vehicleAppId, userId);

            assertThat(res.status()).isEqualTo(ApplicationStatus.SUBMITTED);
            verify(adminAllotmentService).allot(vehicleAppId, AllotmentReferenceType.VEHICLE_APPLICATION);
        }

        @Test
        @DisplayName("Throws BadRequestException when vehicle documents missing")
        void submitVehicleApplication_missingDocs() {
            when(applicationVehicleRepository.findByIdAndOwnerUserId(vehicleAppId, userId))
                    .thenReturn(Optional.of(vehicleApp));
            when(applicationVehicleDocumentRepository.existsByApplicationVehicleAndType(
                    vehicleApp, VehicleOwnershipDocumentType.RC)).thenReturn(false);

            assertThatThrownBy(() -> vehicleApplicationService.submitVehicleApplication(vehicleAppId, userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("RC (Registration Certificate) and Insurance");
        }
    }

    @Nested
    @DisplayName("Admin approve and reject vehicle applications")
    class AdminVehicleTests {

        @Test
        @DisplayName("approveVehicleApplication creates Vehicle and VehicleOwnership for agent")
        void approveVehicleApplication_success() {
            vehicleApp.setStatus(ApplicationStatus.SUBMITTED);
            VehicleApplicationDocument rcDoc = new VehicleApplicationDocument();
            rcDoc.setType(VehicleOwnershipDocumentType.RC);
            rcDoc.setUrl("https://s3.amazonaws.com/rc2.jpg");
            vehicleApp.setDocuments(List.of(rcDoc));

            when(applicationVehicleRepository.findById(vehicleAppId)).thenReturn(Optional.of(vehicleApp));
            when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
            when(vehicleRepository.findByVinNumber("VIN987654321")).thenReturn(Optional.empty());
            when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> i.getArgument(0));
            when(vehicleOwnershipRepository.save(any(VehicleOwnership.class))).thenAnswer(i -> i.getArgument(0));

            vehicleApplicationService.approveVehicleApplication(vehicleAppId, adminId);

            assertThat(vehicleApp.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
            verify(vehicleOwnershipRepository).save(any(VehicleOwnership.class));
            verify(vehicleOwnershipDocumentRepository).saveAll(any());
        }

        @Test
        @DisplayName("rejectVehicleApplication records remarks")
        void rejectVehicleApplication_success() {
            vehicleApp.setStatus(ApplicationStatus.SUBMITTED);
            when(applicationVehicleRepository.findById(vehicleAppId)).thenReturn(Optional.of(vehicleApp));
            when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

            vehicleApplicationService.rejectVehicleApplication(vehicleAppId, adminId, "Expired insurance");

            assertThat(vehicleApp.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
            assertThat(vehicleApp.getRejectionRemarks()).isEqualTo("Expired insurance");
        }
    }
}
