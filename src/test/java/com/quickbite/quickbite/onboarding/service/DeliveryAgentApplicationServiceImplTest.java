package com.quickbite.quickbite.onboarding.service;

import com.quickbite.quickbite.allotment.model.AllotmentReferenceType;
import com.quickbite.quickbite.allotment.service.AdminAllotmentService;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.delivery.model.DeliveryAgentDocumentType;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentDocumentRepository;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.onboarding.dto.deliveryagent.DeliveryAgentApplicationDocumentRequest;
import com.quickbite.quickbite.onboarding.dto.deliveryagent.DeliveryAgentApplicationResponse;
import com.quickbite.quickbite.onboarding.model.*;
import com.quickbite.quickbite.onboarding.model.deliveryagent.DeliveryAgentApplication;
import com.quickbite.quickbite.onboarding.model.deliveryagent.DeliveryAgentApplicationDocument;
import com.quickbite.quickbite.onboarding.model.vehicle.VehicleApplication;
import com.quickbite.quickbite.onboarding.model.vehicle.VehicleApplicationDocument;
import com.quickbite.quickbite.onboarding.repository.*;
import com.quickbite.quickbite.onboarding.service.deliveryagent.DeliveryAgentApplicationServiceImpl;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import com.quickbite.quickbite.vehicle.model.Vehicle;
import com.quickbite.quickbite.vehicle.model.VehicleOwnership;
import com.quickbite.quickbite.vehicle.model.VehicleOwnershipDocumentType;
import com.quickbite.quickbite.vehicle.model.VehicleType;
import com.quickbite.quickbite.vehicle.repository.VehicleOwnershipDocumentRepository;
import com.quickbite.quickbite.vehicle.repository.VehicleOwnershipRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryAgentApplicationServiceImplTest {

    @Mock private DeliveryAgentApplicationRepository applicationRepository;
    @Mock private DeliveryAgentApplicationDocumentRepository applicationDocumentRepository;
    @Mock private DeliveryAgentApplicationVehicleRepository applicationVehicleRepository;
    @Mock private DeliveryAgentApplicationVehicleDocumentRepository applicationVehicleDocumentRepository;
    @Mock private UserRepository userRepository;
    @Mock private DeliveryAgentRepository deliveryAgentRepository;
    @Mock private DeliveryAgentDocumentRepository deliveryAgentDocumentRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private VehicleOwnershipRepository vehicleOwnershipRepository;
    @Mock private VehicleOwnershipDocumentRepository vehicleOwnershipDocumentRepository;
    @Mock private AdminAllotmentService adminAllotmentService;
    @Mock private com.quickbite.quickbite.delivery.repository.DeliveryAgentVerificationHistoryRepository deliveryAgentVerificationHistoryRepository;
    @Mock private com.quickbite.quickbite.vehicle.repository.VehicleOwnershipStatusHistoryRepository vehicleOwnershipStatusHistoryRepository;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DeliveryAgentApplicationServiceImpl applicationService;

    private User agentUser;
    private User adminUser;
    private DeliveryAgentApplication application;
    private VehicleApplication vehicle;
    private UUID userId;
    private UUID adminId;
    private UUID appId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        appId = UUID.randomUUID();

        agentUser = new User();
        agentUser.setId(userId);
        agentUser.setName("Driver Dave");
        agentUser.setEmail("dave@delivery.com");
        agentUser.setActive(false);

        adminUser = new User();
        adminUser.setId(adminId);
        adminUser.setName("Admin Sarah");

        application = new DeliveryAgentApplication();
        application.setId(appId);
        application.setAgent(agentUser);
        application.setStatus(ApplicationStatus.DRAFT);
        application.setCreatedAt(Instant.now());
        application.setUpdatedAt(Instant.now());

        vehicle = new VehicleApplication();
        vehicle.setId(UUID.randomUUID());
        vehicle.setApplication(application);
        vehicle.setVinNumber("VIN123456789");
        vehicle.setNumberPlate("DL-01-AB-1234");
        vehicle.setVehicleType(VehicleType.BIKE);
        vehicle.setBrand("Hero");
        vehicle.setModel("Splendor");
        application.setVehicles(new ArrayList<>(List.of(vehicle)));
    }

    @Nested
    @DisplayName("startApplication")
    class StartApplicationTests {

        @Test
        @DisplayName("Starts new application for eligible user")
        void startApplication_new() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(agentUser));
            when(deliveryAgentRepository.existsByUser(agentUser)).thenReturn(false);
            when(applicationRepository.findByAgentAndStatusIn(eq(agentUser), any())).thenReturn(Optional.empty());
            when(applicationRepository.save(any(DeliveryAgentApplication.class))).thenAnswer(i -> {
                DeliveryAgentApplication app = i.getArgument(0);
                app.setId(appId);
                return app;
            });

            DeliveryAgentApplicationResponse res = applicationService.startApplication(userId);

            assertThat(res.agentId()).isEqualTo(userId);
            assertThat(res.status()).isEqualTo(ApplicationStatus.DRAFT);
        }

        @Test
        @DisplayName("Throws BadRequestException if user is already approved delivery agent")
        void startApplication_alreadyApproved() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(agentUser));
            when(deliveryAgentRepository.existsByUser(agentUser)).thenReturn(true);

            assertThatThrownBy(() -> applicationService.startApplication(userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already an approved delivery agent");
        }
    }

    @Nested
    @DisplayName("Documents and Vehicle Wizard")
    class WizardTests {

        @Test
        @DisplayName("addDocument saves identity doc — returns DeliveryAgentApplicationDocumentResponse")
        void addDocument_savesDoc() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(agentUser));
            when(applicationRepository.findByIdAndAgent(appId, agentUser)).thenReturn(Optional.of(application));
            when(applicationDocumentRepository.findByApplicationAndType(application, DeliveryAgentDocumentType.AADHAR))
                    .thenReturn(Optional.empty());
            when(applicationDocumentRepository.existsByApplicationAndType(application, DeliveryAgentDocumentType.AADHAR))
                    .thenReturn(true);
            when(applicationDocumentRepository.existsByApplicationAndType(application, DeliveryAgentDocumentType.DRIVING_LICENSE))
                    .thenReturn(true);
            when(applicationDocumentRepository.save(any(DeliveryAgentApplicationDocument.class))).thenAnswer(i -> {
                DeliveryAgentApplicationDocument doc = i.getArgument(0);
                doc.setId(UUID.randomUUID());
                return doc;
            });

            DeliveryAgentApplicationDocumentRequest req = new DeliveryAgentApplicationDocumentRequest(
                    DeliveryAgentDocumentType.AADHAR, "https://s3.amazonaws.com/aadhar.jpg");

            // addDocument returns DeliveryAgentApplicationDocumentResponse (not DeliveryAgentApplicationResponse)
            var res = applicationService.addDocument(appId, userId, req);

            assertThat(res).isNotNull();
            verify(applicationDocumentRepository).save(any(DeliveryAgentApplicationDocument.class));
        }

        @Test
        @DisplayName("startVehicleApplication creates new vehicle application linked to main application")
        void startVehicleApplication_createsNew() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(agentUser));
            when(applicationRepository.findByIdAndAgent(appId, agentUser)).thenReturn(Optional.of(application));
            when(applicationVehicleRepository.findByApplication(application)).thenReturn(Optional.empty());
            when(applicationVehicleRepository.save(any(VehicleApplication.class)))
                    .thenAnswer(i -> i.getArgument(0));

            var res = applicationService.startVehicleApplication(appId, userId);

            assertThat(res).isNotNull();
            verify(applicationVehicleRepository).save(any(VehicleApplication.class));
        }

        @Test
        @DisplayName("startVehicleApplication returns existing vehicle application if already started")
        void startVehicleApplication_returnsExisting() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(agentUser));
            when(applicationRepository.findByIdAndAgent(appId, agentUser)).thenReturn(Optional.of(application));
            when(applicationVehicleRepository.findByApplication(application)).thenReturn(Optional.of(vehicle));

            var res = applicationService.startVehicleApplication(appId, userId);

            assertThat(res).isNotNull();
            verify(applicationVehicleRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("submitApplication")
    class SubmitApplicationTests {

        @Test
        @DisplayName("submitApplication succeeds and triggers admin allotment when both steps are complete")
        void submitApplication_success() {
            application.setDocumentsComplete(true);
            application.setVehicleComplete(true);

            when(userRepository.findById(userId)).thenReturn(Optional.of(agentUser));
            when(applicationRepository.findByIdAndAgent(appId, agentUser)).thenReturn(Optional.of(application));
            when(applicationRepository.save(any(DeliveryAgentApplication.class))).thenAnswer(i -> i.getArgument(0));

            DeliveryAgentApplicationResponse res = applicationService.submitApplication(appId, userId);

            assertThat(res.status()).isEqualTo(ApplicationStatus.SUBMITTED);
            verify(adminAllotmentService).allot(appId, AllotmentReferenceType.DELIVERY_AGENT);
        }

        @Test
        @DisplayName("submitApplication throws BadRequestException when documents are incomplete")
        void submitApplication_incompleteDocuments() {
            application.setDocumentsComplete(false);

            when(userRepository.findById(userId)).thenReturn(Optional.of(agentUser));
            when(applicationRepository.findByIdAndAgent(appId, agentUser)).thenReturn(Optional.of(application));

            assertThatThrownBy(() -> applicationService.submitApplication(appId, userId))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("Admin approve and reject")
    class AdminTests {

        @Test
        @DisplayName("approveApplication activates user, promotes agent, creates vehicle and ownership")
        void approveApplication_success() {
            application.setStatus(ApplicationStatus.SUBMITTED);
            DeliveryAgentApplicationDocument aadhar = new DeliveryAgentApplicationDocument();
            aadhar.setType(DeliveryAgentDocumentType.AADHAR);
            aadhar.setUrl("https://s3.amazonaws.com/aadhar.jpg");
            application.setDocuments(List.of(aadhar));

            VehicleApplicationDocument rc = new VehicleApplicationDocument();
            rc.setType(VehicleOwnershipDocumentType.RC);
            rc.setUrl("https://s3.amazonaws.com/rc.jpg");
            vehicle.setDocuments(List.of(rc));

            when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));
            when(applicationVehicleRepository.findByApplication(application)).thenReturn(Optional.of(vehicle));
            when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
            when(deliveryAgentRepository.findByUser(agentUser)).thenReturn(Optional.empty());
            when(deliveryAgentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(vehicleRepository.findByVinNumber("VIN123456789")).thenReturn(Optional.empty());
            when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> i.getArgument(0));
            when(vehicleOwnershipRepository.save(any(VehicleOwnership.class))).thenAnswer(i -> i.getArgument(0));

            applicationService.approveApplication(appId, adminId);

            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(agentUser.isActive()).isTrue();
            verify(deliveryAgentRepository, atLeastOnce()).save(any());
            verify(vehicleOwnershipRepository).save(any());
            verify(deliveryAgentDocumentRepository).saveAll(any());
            verify(vehicleOwnershipDocumentRepository).saveAll(any());
        }

        @Test
        @DisplayName("rejectApplication marks application REJECTED and records remarks")
        void rejectApplication_success() {
            application.setStatus(ApplicationStatus.SUBMITTED);
            when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));
            when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

            applicationService.rejectApplication(appId, adminId, "Unclear Driving License photo");

            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
            assertThat(application.getRejectionRemarks()).isEqualTo("Unclear Driving License photo");
        }
    }
}
