package com.quickbite.quickbite.onboarding.service;

import com.quickbite.quickbite.common.event.restaurantapplication.RestaurantApplicationSubmittedEvent;
import com.quickbite.quickbite.common.event.restaurantapplication.RestaurantApplicationApprovedEvent;
import com.quickbite.quickbite.common.event.restaurantapplication.RestaurantApplicationRejectedEvent;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.onboarding.dto.*;
import com.quickbite.quickbite.onboarding.exception.ApplicationNotFoundException;
import com.quickbite.quickbite.onboarding.exception.ApplicationStateException;
import com.quickbite.quickbite.onboarding.model.*;
import com.quickbite.quickbite.onboarding.repository.*;
import com.quickbite.quickbite.restaurant.model.*;
import com.quickbite.quickbite.restaurant.repository.*;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.AddressRepository;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RestaurantApplicationServiceImpl}.
 * <p>
 * Uses Mockito to mock all repositories and KafkaTemplate so tests run
 * without Spring context, databases, or a running Kafka broker.
 * <p>
 * Organised into @Nested classes grouped by feature area for readability.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RestaurantApplicationServiceImpl")
class RestaurantApplicationServiceImplTest {

    // ---- mocks ----
    @Mock RestaurantApplicationRepository applicationRepository;
    @Mock ApplicationHoursRepository hoursRepository;
    @Mock ApplicationImageRepository imageRepository;
    @Mock ApplicationDocumentRepository documentRepository;
    @Mock UserRepository userRepository;
    @Mock AddressRepository addressRepository;
    @Mock RestaurantRepository restaurantRepository;
    @Mock RestaurantHoursRepository restaurantHoursRepository;
    @Mock RestaurantImageRepository restaurantImageRepository;
    @Mock RestaurantDocumentRepository restaurantDocumentRepository;
    @Mock RestaurantVerificationStatusHistoryRepository statusHistoryRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock com.quickbite.quickbite.allotment.service.AdminAllotmentService adminAllotmentService;

    private RestaurantApplicationServiceImpl service;

    // ---- shared test fixtures ----
    private UUID ownerId;
    private UUID adminId;
    private UUID appId;
    private User owner;
    private User admin;
    private RestaurantApplication draftApp;
    private RestaurantApplication completeApp;   // all flags true, still DRAFT

    @BeforeEach
    void setUp() {
        service = new RestaurantApplicationServiceImpl(
                applicationRepository, hoursRepository, imageRepository,
                documentRepository, userRepository, addressRepository,
                restaurantRepository, restaurantHoursRepository,
                restaurantImageRepository, restaurantDocumentRepository,
                statusHistoryRepository, eventPublisher, adminAllotmentService);

        ownerId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        appId   = UUID.randomUUID();

        owner = new User();
        owner.setId(ownerId);
        owner.setEmail("owner@example.com");
        owner.setName("Test Owner");

        admin = new User();
        admin.setId(adminId);
        admin.setEmail("admin@example.com");
        admin.setName("Test Admin");

        draftApp = new RestaurantApplication();
        draftApp.setId(appId);
        draftApp.setOwner(owner);
        draftApp.setStatus(ApplicationStatus.DRAFT);

        // A fully-complete DRAFT ready for submission
        completeApp = new RestaurantApplication();
        completeApp.setId(appId);
        completeApp.setOwner(owner);
        completeApp.setStatus(ApplicationStatus.DRAFT);
        completeApp.setName("Spice Route");
        completeApp.setDetailsComplete(true);
        completeApp.setAddressComplete(true);
        completeApp.setHoursComplete(true);
        completeApp.setImagesComplete(true);
        completeApp.setDocumentsComplete(true);
    }

    // =========================================================================
    // startApplication
    // =========================================================================
    @Nested
    @DisplayName("startApplication")
    class StartApplicationTests {

        @Test
        @DisplayName("creates a new DRAFT application when owner has no active application")
        void createsDraftWhenNoActive() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.existsByOwnerAndStatusIn(eq(owner), anyList())).thenReturn(false);
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ApplicationResponse response = service.startApplication(ownerId);

            assertThat(response.status()).isEqualTo(ApplicationStatus.DRAFT);
            verify(applicationRepository).save(argThat(app ->
                    app.getOwner().equals(owner) && app.getStatus() == ApplicationStatus.DRAFT));
        }

        @Test
        @DisplayName("throws ApplicationStateException when owner already has an active application")
        void throwsWhenActiveExists() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.existsByOwnerAndStatusIn(eq(owner), anyList())).thenReturn(true);

            assertThatThrownBy(() -> service.startApplication(ownerId))
                    .isInstanceOf(ApplicationStateException.class)
                    .hasMessageContaining("active application");

            verify(applicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when owner does not exist")
        void throwsWhenOwnerMissing() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.startApplication(ownerId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // saveDetails
    // =========================================================================
    @Nested
    @DisplayName("saveDetails")
    class SaveDetailsTests {

        @Test
        @DisplayName("updates name and description and marks detailsComplete=true")
        void updatesDetailsOnDraft() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.of(draftApp));
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var request = new ApplicationDetailsRequest("Spice Route", "Best curry in town");
            ApplicationResponse response = service.saveDetails(appId, ownerId, request);

            assertThat(response.name()).isEqualTo("Spice Route");
            assertThat(response.description()).isEqualTo("Best curry in town");
            assertThat(response.detailsComplete()).isTrue();
        }

        @Test
        @DisplayName("throws ApplicationStateException when application is not in DRAFT status")
        void throwsWhenNotDraft() {
            draftApp.setStatus(ApplicationStatus.SUBMITTED);
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.of(draftApp));

            var request = new ApplicationDetailsRequest("Name", "Desc");

            assertThatThrownBy(() -> service.saveDetails(appId, ownerId, request))
                    .isInstanceOf(ApplicationStateException.class)
                    .hasMessageContaining("DRAFT");

            verify(applicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ApplicationNotFoundException when application does not belong to owner")
        void throwsWhenNotOwner() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.saveDetails(appId, ownerId, new ApplicationDetailsRequest("N", "D")))
                    .isInstanceOf(ApplicationNotFoundException.class);
        }
    }

    // =========================================================================
    // saveAddress
    // =========================================================================
    @Nested
    @DisplayName("saveAddress")
    class SaveAddressTests {

        @Test
        @DisplayName("saves address fields and marks addressComplete=true")
        void savesAddressFields() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.of(draftApp));
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var request = new ApplicationAddressRequest(
                    "MG Road", "Bengaluru", "Karnataka", "India",
                    "560001", "12", "Brigade Tower", "Near Metro",
                    12.9716, 77.5946);

            ApplicationResponse response = service.saveAddress(appId, ownerId, request);

            assertThat(response.addressComplete()).isTrue();
            assertThat(response.address()).isNotNull();
            assertThat(response.address().city()).isEqualTo("Bengaluru");
        }

        @Test
        @DisplayName("creates a JTS Point when latitude and longitude are provided")
        void createsGeometryPoint() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.of(draftApp));
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var request = new ApplicationAddressRequest(
                    "Street", "City", "State", "India",
                    "560001", null, null, null, 12.9716, 77.5946);

            service.saveAddress(appId, ownerId, request);

            ArgumentCaptor<RestaurantApplication> captor = ArgumentCaptor.forClass(RestaurantApplication.class);
            verify(applicationRepository).save(captor.capture());
            assertThat(captor.getValue().getAddressLocation()).isNotNull();
            assertThat(captor.getValue().getAddressLocation().getY()).isEqualTo(12.9716);
            assertThat(captor.getValue().getAddressLocation().getX()).isEqualTo(77.5946);
        }
    }

    // =========================================================================
    // saveHours
    // =========================================================================
    @Nested
    @DisplayName("saveHours")
    class SaveHoursTests {

        @Test
        @DisplayName("deletes existing hours and saves new ones, sets hoursComplete=true")
        void replacesHoursAndSetsFlag() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.of(draftApp));
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var request = new ApplicationHoursRequest(List.of(
                    new ApplicationHoursRequest.HourEntry(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(22, 0)),
                    new ApplicationHoursRequest.HourEntry(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(22, 0))
            ));

            service.saveHours(appId, ownerId, request);

            verify(hoursRepository).deleteAllByApplication(draftApp);
            verify(hoursRepository).saveAll(argThat(list -> ((List<?>) list).size() == 2));

            ArgumentCaptor<RestaurantApplication> captor = ArgumentCaptor.forClass(RestaurantApplication.class);
            verify(applicationRepository).save(captor.capture());
            assertThat(captor.getValue().isHoursComplete()).isTrue();
        }

        @Test
        @DisplayName("sets hoursComplete=false when empty hours list is provided")
        void setsIncompleteOnEmptyHours() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.of(draftApp));
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var request = new ApplicationHoursRequest(List.of());

            service.saveHours(appId, ownerId, request);

            ArgumentCaptor<RestaurantApplication> captor = ArgumentCaptor.forClass(RestaurantApplication.class);
            verify(applicationRepository).save(captor.capture());
            assertThat(captor.getValue().isHoursComplete()).isFalse();
        }
    }

    // =========================================================================
    // addImage / removeImage
    // =========================================================================
    @Nested
    @DisplayName("addImage / removeImage")
    class ImageTests {

        @Test
        @DisplayName("saves image and sets imagesComplete=true when at least one image exists")
        void addsImageAndSetsFlag() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.of(draftApp));
            when(imageRepository.countByApplication(draftApp)).thenReturn(1L);
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var request = new ApplicationImageRequest("https://s3.example.com/img.jpg", 0);
            ApplicationResponse response = service.addImage(appId, ownerId, request);

            assertThat(response.imagesComplete()).isTrue();
            verify(imageRepository).save(any(ApplicationImage.class));
        }

        @Test
        @DisplayName("sets imagesComplete=false after removing the last image")
        void setsIncompleteAfterLastImageRemoved() {
            ApplicationImage image = new ApplicationImage();
            image.setId(UUID.randomUUID());

            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.of(draftApp));
            when(imageRepository.findByIdAndApplication(image.getId(), draftApp)).thenReturn(Optional.of(image));
            when(imageRepository.countByApplication(draftApp)).thenReturn(0L);

            service.removeImage(appId, ownerId, image.getId());

            ArgumentCaptor<RestaurantApplication> captor = ArgumentCaptor.forClass(RestaurantApplication.class);
            verify(applicationRepository).save(captor.capture());
            assertThat(captor.getValue().isImagesComplete()).isFalse();
        }

        @Test
        @DisplayName("throws ApplicationNotFoundException when image is not found")
        void throwsWhenImageMissing() {
            UUID imageId = UUID.randomUUID();
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.of(draftApp));
            when(imageRepository.findByIdAndApplication(imageId, draftApp)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeImage(appId, ownerId, imageId))
                    .isInstanceOf(ApplicationNotFoundException.class)
                    .hasMessageContaining("Image not found");
        }
    }

    // =========================================================================
    // addDocument
    // =========================================================================
    @Nested
    @DisplayName("addDocument")
    class AddDocumentTests {

        @Test
        @DisplayName("marks documentsComplete=true when all 4 mandatory types are present")
        void setsCompleteWhenAllMandatoryPresent() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.of(draftApp));
            when(documentRepository.findByApplicationAndType(any(), any())).thenReturn(Optional.empty());
            when(documentRepository.existsByApplicationAndType(eq(draftApp), any())).thenReturn(true);
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var request = new ApplicationDocumentRequest(RestaurantDocumentType.FSSAI_LICENSE, "https://s3/fssai.pdf");
            ApplicationResponse response = service.addDocument(appId, ownerId, request);

            assertThat(response.documentsComplete()).isTrue();
        }

        @Test
        @DisplayName("documentsComplete=false when at least one mandatory type is still missing")
        void incompleteWhenOneMandatoryMissing() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.of(draftApp));
            when(documentRepository.findByApplicationAndType(any(), any())).thenReturn(Optional.empty());
            // Use lenient stubs: allMatch short-circuits as soon as one type returns false,
            // so the remaining stubs may not be called — that is intentional behavior, not a test defect.
            lenient().when(documentRepository.existsByApplicationAndType(eq(draftApp), eq(RestaurantDocumentType.FSSAI_LICENSE))).thenReturn(true);
            lenient().when(documentRepository.existsByApplicationAndType(eq(draftApp), eq(RestaurantDocumentType.GST))).thenReturn(false);
            lenient().when(documentRepository.existsByApplicationAndType(eq(draftApp), eq(RestaurantDocumentType.PAN))).thenReturn(true);
            lenient().when(documentRepository.existsByApplicationAndType(eq(draftApp), eq(RestaurantDocumentType.BANK_PROOF))).thenReturn(true);
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var request = new ApplicationDocumentRequest(RestaurantDocumentType.FSSAI_LICENSE, "https://s3/fssai.pdf");
            ApplicationResponse response = service.addDocument(appId, ownerId, request);

            assertThat(response.documentsComplete()).isFalse();
        }


        @Test
        @DisplayName("upserts document — deletes existing entry for same type before inserting")
        void upsertDeletesExistingType() {
            ApplicationDocument existing = new ApplicationDocument();
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.of(draftApp));
            when(documentRepository.findByApplicationAndType(draftApp, RestaurantDocumentType.PAN))
                    .thenReturn(Optional.of(existing));
            when(documentRepository.existsByApplicationAndType(any(), any())).thenReturn(true);
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.addDocument(appId, ownerId, new ApplicationDocumentRequest(RestaurantDocumentType.PAN, "new.pdf"));

            verify(documentRepository).delete(existing);
            verify(documentRepository).save(any(ApplicationDocument.class));
        }
    }

    // =========================================================================
    // submitApplication
    // =========================================================================
    @Nested
    @DisplayName("submitApplication")
    class SubmitApplicationTests {

        @Test
        @DisplayName("changes status to SUBMITTED and publishes Kafka event when all steps complete")
        void submitsSuccessfully() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.of(completeApp));
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ApplicationResponse response = service.submitApplication(appId, ownerId);

            assertThat(response.status()).isEqualTo(ApplicationStatus.SUBMITTED);

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            RestaurantApplicationSubmittedEvent event =
                    (RestaurantApplicationSubmittedEvent) eventCaptor.getValue();
            assertThat(event.applicationId()).isEqualTo(appId);
            assertThat(event.ownerId()).isEqualTo(ownerId);
            assertThat(event.restaurantName()).isEqualTo("Spice Route");
        }

        @Test
        @DisplayName("throws ApplicationStateException when details step is incomplete")
        void throwsWhenDetailsIncomplete() {
            completeApp.setDetailsComplete(false);
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.of(completeApp));

            assertThatThrownBy(() -> service.submitApplication(appId, ownerId))
                    .isInstanceOf(ApplicationStateException.class)
                    .hasMessageContaining("incomplete");

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("throws ApplicationStateException when documents step is incomplete")
        void throwsWhenDocumentsIncomplete() {
            completeApp.setDocumentsComplete(false);
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.of(completeApp));

            assertThatThrownBy(() -> service.submitApplication(appId, ownerId))
                    .isInstanceOf(ApplicationStateException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("throws ApplicationStateException when application is not in DRAFT status")
        void throwsWhenNotDraft() {
            completeApp.setStatus(ApplicationStatus.SUBMITTED);
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.of(completeApp));

            assertThatThrownBy(() -> service.submitApplication(appId, ownerId))
                    .isInstanceOf(ApplicationStateException.class)
                    .hasMessageContaining("DRAFT");
        }
    }

    // =========================================================================
    // reopenApplication
    // =========================================================================
    @Nested
    @DisplayName("reopenApplication")
    class ReopenApplicationTests {

        @Test
        @DisplayName("changes status to DRAFT and clears rejection remarks when REJECTED")
        void reopensRejected() {
            draftApp.setStatus(ApplicationStatus.REJECTED);
            draftApp.setRejectionRemarks("Missing documents");
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.of(draftApp));
            when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ApplicationResponse response = service.reopenApplication(appId, ownerId);

            assertThat(response.status()).isEqualTo(ApplicationStatus.DRAFT);
            assertThat(response.rejectionRemarks()).isNull();
        }

        @Test
        @DisplayName("throws ApplicationStateException when application is SUBMITTED")
        void throwsWhenSubmitted() {
            draftApp.setStatus(ApplicationStatus.SUBMITTED);
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.of(draftApp));

            assertThatThrownBy(() -> service.reopenApplication(appId, ownerId))
                    .isInstanceOf(ApplicationStateException.class)
                    .hasMessageContaining("REJECTED");
        }

        @Test
        @DisplayName("throws ApplicationStateException when application is APPROVED")
        void throwsWhenApproved() {
            draftApp.setStatus(ApplicationStatus.APPROVED);
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(applicationRepository.findByIdAndOwner(appId, owner)).thenReturn(Optional.of(draftApp));

            assertThatThrownBy(() -> service.reopenApplication(appId, ownerId))
                    .isInstanceOf(ApplicationStateException.class);
        }
    }

    // =========================================================================
    // rejectApplication (admin)
    // =========================================================================
    @Nested
    @DisplayName("rejectApplication")
    class RejectApplicationTests {

        @Test
        @DisplayName("sets status REJECTED, stores remarks, publishes Kafka event")
        void rejectsSubmittedApplication() {
            completeApp.setStatus(ApplicationStatus.SUBMITTED);
            when(applicationRepository.findById(appId)).thenReturn(Optional.of(completeApp));
            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

            service.rejectApplication(appId, adminId, "Incomplete FSSAI license");

            ArgumentCaptor<RestaurantApplication> captor = ArgumentCaptor.forClass(RestaurantApplication.class);
            verify(applicationRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ApplicationStatus.REJECTED);
            assertThat(captor.getValue().getRejectionRemarks()).isEqualTo("Incomplete FSSAI license");
            assertThat(captor.getValue().getReviewedBy()).isEqualTo(admin);

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            RestaurantApplicationRejectedEvent event = (RestaurantApplicationRejectedEvent) eventCaptor.getValue();
            assertThat(event.applicationId()).isEqualTo(appId);
            assertThat(event.rejectionRemarks()).isEqualTo("Incomplete FSSAI license");
        }

        @Test
        @DisplayName("also rejects UNDER_REVIEW applications")
        void rejectsUnderReviewApplication() {
            completeApp.setStatus(ApplicationStatus.UNDER_REVIEW);
            when(applicationRepository.findById(appId)).thenReturn(Optional.of(completeApp));
            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

            service.rejectApplication(appId, adminId, "Brand new remarks");

            verify(applicationRepository).save(argThat(app ->
                    app.getStatus() == ApplicationStatus.REJECTED));
        }

        @Test
        @DisplayName("throws ApplicationStateException when application is already APPROVED")
        void throwsWhenAlreadyApproved() {
            completeApp.setStatus(ApplicationStatus.APPROVED);
            when(applicationRepository.findById(appId)).thenReturn(Optional.of(completeApp));

            assertThatThrownBy(() -> service.rejectApplication(appId, adminId, "remarks"))
                    .isInstanceOf(ApplicationStateException.class)
                    .hasMessageContaining("SUBMITTED or UNDER_REVIEW");

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("throws ApplicationNotFoundException when application does not exist")
        void throwsWhenApplicationMissing() {
            when(applicationRepository.findById(appId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.rejectApplication(appId, adminId, "remarks"))
                    .isInstanceOf(ApplicationNotFoundException.class);
        }
    }

    // =========================================================================
    // approveApplication (admin)
    // =========================================================================
    @Nested
    @DisplayName("approveApplication")
    class ApproveApplicationTests {

        private Restaurant savedRestaurant;
        private Address savedAddress;

        @BeforeEach
        void setUpApprovalMocks() {
            completeApp.setStatus(ApplicationStatus.SUBMITTED);
            completeApp.setAddressStreet("MG Road");
            completeApp.setAddressCity("Bengaluru");
            completeApp.setAddressState("Karnataka");
            completeApp.setAddressCountry("India");

            savedAddress = new Address();
            savedAddress.setId(UUID.randomUUID());

            savedRestaurant = new Restaurant();
            savedRestaurant.setId(UUID.randomUUID());
            savedRestaurant.setName("Spice Route");
        }

        @Test
        @DisplayName("creates Restaurant, updates application status, publishes approved event")
        void approvesAndCreatesRestaurant() {
            when(applicationRepository.findById(appId)).thenReturn(Optional.of(completeApp));
            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
            when(addressRepository.save(any())).thenReturn(savedAddress);
            when(restaurantRepository.save(any())).thenReturn(savedRestaurant);

            service.approveApplication(appId, adminId);

            // Application should be saved as APPROVED
            ArgumentCaptor<RestaurantApplication> appCaptor = ArgumentCaptor.forClass(RestaurantApplication.class);
            verify(applicationRepository).save(appCaptor.capture());
            assertThat(appCaptor.getValue().getStatus()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(appCaptor.getValue().getReviewedBy()).isEqualTo(admin);
            assertThat(appCaptor.getValue().getRestaurant()).isEqualTo(savedRestaurant);

            // Restaurant must be saved
            verify(restaurantRepository).save(any(Restaurant.class));

            // Kafka event published to RESTAURANT_APPROVED topic
            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            RestaurantApplicationApprovedEvent event = (RestaurantApplicationApprovedEvent) eventCaptor.getValue();
            assertThat(event.applicationId()).isEqualTo(appId);
            assertThat(event.ownerId()).isEqualTo(ownerId);
        }

        @Test
        @DisplayName("throws ApplicationStateException when application is in DRAFT status")
        void throwsWhenDraft() {
            completeApp.setStatus(ApplicationStatus.DRAFT);
            when(applicationRepository.findById(appId)).thenReturn(Optional.of(completeApp));

            assertThatThrownBy(() -> service.approveApplication(appId, adminId))
                    .isInstanceOf(ApplicationStateException.class)
                    .hasMessageContaining("SUBMITTED or UNDER_REVIEW");

            verify(restaurantRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("also approves UNDER_REVIEW applications")
        void approvesUnderReview() {
            completeApp.setStatus(ApplicationStatus.UNDER_REVIEW);
            when(applicationRepository.findById(appId)).thenReturn(Optional.of(completeApp));
            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
            when(addressRepository.save(any())).thenReturn(savedAddress);
            when(restaurantRepository.save(any())).thenReturn(savedRestaurant);

            assertThatCode(() -> service.approveApplication(appId, adminId))
                    .doesNotThrowAnyException();

            verify(restaurantRepository).save(any(Restaurant.class));
        }

        @Test
        @DisplayName("migrates hours, images and documents from application to restaurant")
        void migratesAllCollections() {
            // Set up collections on the application
            ApplicationHours h = new ApplicationHours();
            h.setDayOfWeek(DayOfWeek.MONDAY);
            h.setOpenTime(LocalTime.of(9, 0));
            h.setCloseTime(LocalTime.of(22, 0));
            completeApp.getHours().add(h);

            ApplicationImage img = new ApplicationImage();
            img.setImageUrl("https://s3/img.jpg");
            img.setDisplayOrder(0);
            completeApp.getImages().add(img);

            ApplicationDocument doc = new ApplicationDocument();
            doc.setType(RestaurantDocumentType.FSSAI_LICENSE);
            doc.setUrl("https://s3/fssai.pdf");
            completeApp.getDocuments().add(doc);

            when(applicationRepository.findById(appId)).thenReturn(Optional.of(completeApp));
            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
            when(addressRepository.save(any())).thenReturn(savedAddress);
            when(restaurantRepository.save(any())).thenReturn(savedRestaurant);

            service.approveApplication(appId, adminId);

            verify(restaurantHoursRepository).saveAll(argThat(list -> ((List<?>) list).size() == 1));
            verify(restaurantImageRepository).saveAll(argThat(list -> ((List<?>) list).size() == 1));
            verify(restaurantDocumentRepository).saveAll(argThat(list -> ((List<?>) list).size() == 1));
            verify(statusHistoryRepository).save(any(RestaurantVerificationStatusHistory.class));
        }
    }
}
