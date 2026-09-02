package com.quickbite.quickbite.onboarding.service.vehicle;

import com.quickbite.quickbite.allotment.model.AdminAllotment;
import com.quickbite.quickbite.allotment.model.AllotmentReferenceType;
import com.quickbite.quickbite.allotment.service.AdminAllotmentService;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.common.event.vehicleapplication.VehicleApplicationApprovedEvent;
import com.quickbite.quickbite.common.event.vehicleapplication.VehicleApplicationRejectedEvent;
import com.quickbite.quickbite.common.event.vehicleapplication.VehicleApplicationSubmittedEvent;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.common.model.DocumentVerificationStatus;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.onboarding.dto.vehicle.*;
import com.quickbite.quickbite.onboarding.exception.ApplicationNotFoundException;
import com.quickbite.quickbite.onboarding.exception.ApplicationStateException;
import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.model.deliveryagent.DeliveryAgentApplication;
import com.quickbite.quickbite.onboarding.model.vehicle.VehicleApplication;
import com.quickbite.quickbite.onboarding.model.vehicle.VehicleApplicationDocument;
import com.quickbite.quickbite.onboarding.repository.DeliveryAgentApplicationRepository;
import com.quickbite.quickbite.onboarding.repository.DeliveryAgentApplicationVehicleDocumentRepository;
import com.quickbite.quickbite.onboarding.repository.DeliveryAgentApplicationVehicleRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import com.quickbite.quickbite.vehicle.model.*;
import com.quickbite.quickbite.vehicle.repository.VehicleOwnershipDocumentRepository;
import com.quickbite.quickbite.vehicle.repository.VehicleOwnershipRepository;
import com.quickbite.quickbite.vehicle.repository.VehicleOwnershipStatusHistoryRepository;
import com.quickbite.quickbite.vehicle.repository.VehicleRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class VehicleApplicationServiceImpl implements VehicleApplicationService, AdminVehicleApplicationService {

    private final static List<VehicleOwnershipDocumentType> MANDATORY_VEHICLE_DOCUMENT_TYPES =
            List.of(VehicleOwnershipDocumentType.RC, VehicleOwnershipDocumentType.INSURANCE);

    private final UserRepository userRepository;
    private final DeliveryAgentApplicationRepository deliveryAgentApplicationRepository;
    private final DeliveryAgentApplicationVehicleRepository applicationVehicleRepository;
    private final DeliveryAgentApplicationVehicleDocumentRepository applicationVehicleDocumentRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleOwnershipRepository vehicleOwnershipRepository;
    private final VehicleOwnershipDocumentRepository vehicleOwnershipDocumentRepository;
    private final VehicleOwnershipStatusHistoryRepository vehicleOwnershipStatusHistoryRepository;
    private final AdminAllotmentService adminAllotmentService;
    private final ApplicationEventPublisher eventPublisher;

    public VehicleApplicationServiceImpl(
            UserRepository userRepository,
            DeliveryAgentApplicationRepository deliveryAgentApplicationRepository,
            DeliveryAgentApplicationVehicleRepository applicationVehicleRepository,
            DeliveryAgentApplicationVehicleDocumentRepository applicationVehicleDocumentRepository,
            DeliveryAgentRepository deliveryAgentRepository,
            VehicleRepository vehicleRepository,
            VehicleOwnershipRepository vehicleOwnershipRepository,
            VehicleOwnershipDocumentRepository vehicleOwnershipDocumentRepository,
            VehicleOwnershipStatusHistoryRepository vehicleOwnershipStatusHistoryRepository,
            AdminAllotmentService adminAllotmentService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.deliveryAgentApplicationRepository = deliveryAgentApplicationRepository;
        this.applicationVehicleRepository = applicationVehicleRepository;
        this.applicationVehicleDocumentRepository = applicationVehicleDocumentRepository;
        this.deliveryAgentRepository = deliveryAgentRepository;
        this.vehicleRepository = vehicleRepository;
        this.vehicleOwnershipRepository = vehicleOwnershipRepository;
        this.vehicleOwnershipDocumentRepository = vehicleOwnershipDocumentRepository;
        this.vehicleOwnershipStatusHistoryRepository = vehicleOwnershipStatusHistoryRepository;
        this.adminAllotmentService = adminAllotmentService;
        this.eventPublisher = eventPublisher;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Approved Delivery Agent Operations
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Checks if a vehicle with the given VIN number exists in the system.
     * @param vinNumber The VIN number to check
     * @return A response indicating whether the vehicle exists and its details if it does
     */
    @Override
    @Transactional(readOnly = true)
    public CheckVinResponse checkVin(String vinNumber) {
        if (vinNumber == null || vinNumber.isBlank()) {
            throw new BadRequestException("VIN number is required");
        }

        return vehicleRepository.findByVinNumber(vinNumber.trim())
                .map(v -> new CheckVinResponse(true, v.getId(), v.getVinNumber(), v.getNumberPlate(), v.getBrand(), v.getModel(), v.getVehicleType()))
                .orElseGet(() -> CheckVinResponse.notFound(vinNumber.trim()));
    }

    /**
     * Retrieves all standalone vehicle applications (Flow 2) that belong to the approved delivery agent.
     * @param agentUserId The user ID of the delivery agent
     * @return A list of vehicle application summaries
     */
    @Override
    @Transactional(readOnly = true)
    public List<VehicleApplicationResponse> getMyVehicleApplications(UUID agentUserId) {
        // Returns standalone vehicle applications (Flow 2) that belong to this approved agent
        DeliveryAgent agent = loadDeliveryAgentByUserId(agentUserId);
        return applicationVehicleRepository.findByDeliveryAgent(agent).stream()
                .map(VehicleApplicationResponse::from)
                .toList();
    }

    /**
     * Retrieves a specific vehicle application for the approved delivery agent.
     * This method works for both onboarding (Flow 1) and standalone (Flow 2) vehicle applications.
     * @param vehicleAppId The ID of the vehicle application
     * @param agentUserId The user ID of the delivery agent
     * @return The vehicle application details
     */
    @Override
    @Transactional(readOnly = true)
    public VehicleApplicationResponse getVehicleApplication(UUID vehicleAppId, UUID agentUserId) {
        // Works for both onboarding (Flow 1) and standalone (Flow 2) via ownership query
        VehicleApplication vehicleApp = loadVehicleApplicationForUser(vehicleAppId, agentUserId);
        return VehicleApplicationResponse.from(vehicleApp);
    }

    /**
     * Starts a new standalone vehicle application (Flow 2) for the approved delivery agent.
     * @param agentUserId The user ID of the delivery agent
     * @return The newly created vehicle application details
     */
    @Override
    public VehicleApplicationResponse startApplication(UUID agentUserId) {
        // Only approved agents can create standalone vehicle applications
        DeliveryAgent agent = loadDeliveryAgentByUserId(agentUserId);

        VehicleApplication vehicleApp = new VehicleApplication();
        vehicleApp.setDeliveryAgent(agent);
        vehicleApp.setApplication(null);
        vehicleApp.setStatus(ApplicationStatus.DRAFT);

        VehicleApplication saved = applicationVehicleRepository.save(vehicleApp);
        return VehicleApplicationResponse.from(saved);
    }

    /**
     * Saves or updates vehicle details for a draft vehicle application.
     * For onboarding (Flow 1), this will also update the onboard DeliveryAgentApplication's vehicleComplete flag.
     * For standalone (Flow 2), this will only update the vehicle application itself.
     * @param id The ID of the vehicle application
     * @param agentUserId The user ID of the delivery agent
     * @param request The request containing vehicle details
     * @return The updated vehicle application details
     */
    @Override
    public VehicleApplicationDetailsResponse saveVehicleDetails(UUID id, UUID agentUserId, DeliveryAgentApplicationVehicleRequest request) {
        VehicleApplication vehicleApp = loadVehicleApplicationForUser(id, agentUserId);
        DeliveryAgentApplication onboardApp = vehicleApp.getApplication();

        if (onboardApp != null) {
            onboardApp = loadDraftDeliveryAgentApplication(vehicleApp);
        } else {
            loadDraftVehicleApplication(vehicleApp);
        }

        Optional<Vehicle> existing = vehicleRepository.findByVinNumber(request.vinNumber().trim());
        vehicleApp.setExistingVehicle(existing.orElse(null));
        vehicleApp.setOwnershipTransferred(request.isOwnershipTransferred());
        vehicleApp.setVinNumber(request.vinNumber().trim());
        vehicleApp.setNumberPlate(request.numberPlate().trim());
        vehicleApp.setVehicleType(request.vehicleType());
        vehicleApp.setBrand(request.brand().trim());
        vehicleApp.setModel(request.model().trim());

        VehicleApplication saved = applicationVehicleRepository.save(vehicleApp);

        // If this vehicle belongs to a main DeliveryAgentApplication, update vehicleComplete flag
        if (onboardApp != null) {
            updateOnboardApplicationVehicleComplete(onboardApp, saved);
        }

        return VehicleApplicationDetailsResponse.from(saved);
    }

    /**
     * Retrieves vehicle application details for a given vehicle application ID and agent user ID.
     * This method works for both onboarding (Flow 1) and standalone (Flow 2) vehicle applications.
     * @param vehicleAppId The ID of the vehicle application
     * @param agentUserId The user ID of the delivery agent
     * @return The vehicle application details
     */
    @Override
    @Transactional(readOnly = true)
    public VehicleApplicationDetailsResponse getVehicleDetails(UUID vehicleAppId, UUID agentUserId) {
        VehicleApplication vehicleApp = loadVehicleApplicationForUser(vehicleAppId, agentUserId);
        return VehicleApplicationDetailsResponse.from(vehicleApp);
    }

    /**
     * Saves a vehicle document for a given vehicle application ID and agent user ID.
     * If a document of the same type already exists, it will be replaced.
     * For onboarding (Flow 1), this will also update the onboard DeliveryAgentApplication's vehicleComplete flag.
     * For standalone (Flow 2), this will only update the vehicle application itself.
     * @param vehicleAppId The ID of the vehicle application
     * @param agentUserId The user ID of the delivery agent
     * @param request The request containing document details
     * @return The saved vehicle document details
     */
    @Override
    public VehicleApplicationDocumentResponse saveVehicleDocument(UUID vehicleAppId, UUID agentUserId, DeliveryAgentApplicationVehicleDocumentRequest request) {
        VehicleApplication vehicleApp = loadVehicleApplicationForUser(vehicleAppId, agentUserId);
        DeliveryAgentApplication onboardApp = vehicleApp.getApplication();

        if (onboardApp != null) {
            onboardApp = loadDraftDeliveryAgentApplication(vehicleApp);
        } else {
            loadDraftVehicleApplication(vehicleApp);
        }

        applicationVehicleDocumentRepository
                .findByApplicationVehicleAndType(vehicleApp, request.type())
                .ifPresent(applicationVehicleDocumentRepository::delete);

        VehicleApplicationDocument newDoc = new VehicleApplicationDocument();
        newDoc.setApplicationVehicle(vehicleApp);
        newDoc.setType(request.type());
        newDoc.setUrl(request.url());
        VehicleApplicationDocument saved = applicationVehicleDocumentRepository.save(newDoc);

        // Sync vehicleComplete on onboard application if this is an onboarding vehicle
        if (onboardApp != null) {
            updateOnboardApplicationVehicleComplete(onboardApp, vehicleApp);
        }

        return VehicleApplicationDocumentResponse.from(saved);
    }

    /**
     * Retrieves all vehicle documents for a given vehicle application ID and agent user ID.
     * This method works for both onboarding (Flow 1) and standalone (Flow 2) vehicle applications.
     * @param vehicleAppId The ID of the vehicle application
     * @param agentUserId The user ID of the delivery agent
     * @return A list of vehicle document details
     */
    @Override
    @Transactional(readOnly = true)
    public List<VehicleApplicationDocumentResponse> getVehicleDocuments(UUID vehicleAppId, UUID agentUserId) {
        VehicleApplication vehicleApp = loadVehicleApplicationForUser(vehicleAppId, agentUserId);
        return applicationVehicleDocumentRepository.findByApplicationVehicle(vehicleApp).stream()
                .map(VehicleApplicationDocumentResponse::from)
                .toList();
    }

    /**
     * Removes a vehicle document of a specific type for a given vehicle application ID and agent user ID.
     * For onboarding (Flow 1), this will also update the onboard DeliveryAgentApplication's vehicleComplete flag.
     * For standalone (Flow 2), this will only update the vehicle application itself.
     * @param vehicleAppId The ID of the vehicle application
     * @param agentUserId The user ID of the delivery agent
     * @param type The type of the document to be removed
     */
    @Override
    public void removeVehicleDocument(UUID vehicleAppId, UUID agentUserId, VehicleOwnershipDocumentType type) {
        VehicleApplication vehicleApp = loadVehicleApplicationForUser(vehicleAppId, agentUserId);
        DeliveryAgentApplication onboardApp = vehicleApp.getApplication();

        if (onboardApp != null) {
            onboardApp = loadDraftDeliveryAgentApplication(vehicleApp);
        } else {
            loadDraftVehicleApplication(vehicleApp);
        }

        VehicleApplicationDocument doc = applicationVehicleDocumentRepository
                .findByApplicationVehicleAndType(vehicleApp, type)
                .orElseThrow(() -> new BadRequestException("No document of type " + type + " found."));
        applicationVehicleDocumentRepository.delete(doc);

        // Sync vehicleComplete on onboard application
        if (onboardApp != null) {
            updateOnboardApplicationVehicleComplete(onboardApp, vehicleApp);
        }
    }

    /**
     * Submits a vehicle application for review. The application must have all mandatory documents (RC and Insurance) before submission.
     * Standalone vehicle applications (Flow 2) are submitted independently, while onboarding vehicle applications (Flow 1) are submitted as part of the onboard DeliveryAgentApplication.
     * @param vehicleAppId The ID of the vehicle application to be submitted
     * @param agentUserId The user ID of the delivery agent submitting the application
     * @return The submitted vehicle application details
     */
    @Override
    public VehicleApplicationResponse submitVehicleApplication(UUID vehicleAppId, UUID agentUserId) {
        VehicleApplication vehicleApp = loadDraftVehicleApplication(vehicleAppId, agentUserId);

        if (vehicleApp.getVinNumber() == null || vehicleApp.getVinNumber().isBlank()
                || vehicleApp.getNumberPlate() == null || vehicleApp.getNumberPlate().isBlank()) {
            throw new BadRequestException("Vehicle specifications (VIN number and Number Plate) must be provided before submission.");
        }

        boolean allMandatoryDocsPresent = MANDATORY_VEHICLE_DOCUMENT_TYPES.stream()
                .allMatch(type -> applicationVehicleDocumentRepository
                        .existsByApplicationVehicleAndType(vehicleApp, type));

        if (!allMandatoryDocsPresent) {
            throw new BadRequestException("Both RC (Registration Certificate) and Insurance documents are required before submitting the vehicle application.");
        }

        // Only standalone vehicle apps are submitted independently.
        // Onboarding vehicle apps are submitted as part of the onboard DeliveryAgentApplication.
        if (vehicleApp.getApplication() != null) {
            throw new BadRequestException("This vehicle application is part of the main onboarding. Please submit the main application instead.");
        }

        vehicleApp.setStatus(ApplicationStatus.SUBMITTED);
        VehicleApplication saved = applicationVehicleRepository.save(vehicleApp);

        List<AdminAllotment> adminAllotments = adminAllotmentService.allot(saved.getId(), AllotmentReferenceType.VEHICLE_APPLICATION);

        eventPublisher.publishEvent(new VehicleApplicationSubmittedEvent(
                saved.getId(),
                agentUserId,
                saved.getDeliveryAgent().getId(),
                saved.getBrand() + " " + saved.getModel(),
                adminAllotments.stream()
                        .map(aa -> aa.getAdmin().getId())
                        .toList(),
                Instant.now()
        ));

        return VehicleApplicationResponse.from(saved);
    }

    /**
     * Reopens a previously rejected vehicle application, allowing the delivery agent to make changes and resubmit it for review.
     * Only vehicle applications with a status of REJECTED can be reopened.
     * @param vehicleAppId The ID of the vehicle application to be reopened
     * @param agentUserId The user ID of the delivery agent reopening the application
     * @return The reopened vehicle application details
     */
    @Override
    public VehicleApplicationResponse reopenVehicleApplication(UUID vehicleAppId, UUID agentUserId) {
        VehicleApplication vehicleApp = loadVehicleApplicationForUser(vehicleAppId, agentUserId);

        if (vehicleApp.getApplication() != null) {
            throw new BadRequestException("This vehicle application is part of main onboarding. Please reopen the main delivery agent application instead.");
        }

        if (vehicleApp.getStatus() != ApplicationStatus.REJECTED) {
            throw new BadRequestException("Only REJECTED vehicle applications can be reopened. Current status: " + vehicleApp.getStatus());
        }

        vehicleApp.setStatus(ApplicationStatus.DRAFT);
        return VehicleApplicationResponse.from(applicationVehicleRepository.save(vehicleApp));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Admin Operations
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CursorPage<VehicleApplicationSummaryResponse> listVehicleApplications(ApplicationStatus status, UUID cursor, int size) {
        int pageSize = Math.clamp(size, 1, 100);
        List<VehicleApplication> apps = applicationVehicleRepository.findStandaloneWithCursor(status, cursor, Limit.of(pageSize + 1));

        return CursorPage.of(
                apps.stream().map(VehicleApplicationSummaryResponse::from).toList(),
                pageSize,
                VehicleApplicationSummaryResponse::id
        );
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleApplicationResponse getVehicleApplicationAsAdmin(UUID vehicleAppId) {
        VehicleApplication vehicleApp = applicationVehicleRepository.findById(vehicleAppId)
                .orElseThrow(() -> new ApplicationNotFoundException("Vehicle application not found: " + vehicleAppId));
        return VehicleApplicationResponse.from(vehicleApp);
    }

    @Override
    public void approveVehicleApplication(UUID vehicleAppId, UUID adminId) {
        VehicleApplication vehicleApp = applicationVehicleRepository.findById(vehicleAppId)
                .orElseThrow(() -> new ApplicationNotFoundException("Vehicle application not found: " + vehicleAppId));

        if (vehicleApp.getDeliveryAgent() == null) {
            throw new BadRequestException("This vehicle application belongs to onboarding. Please approve the main delivery agent application.");
        }

        checkApplicationStatus(vehicleApp);

        User admin = loadUser(adminId);

        // 1. Resolve or create physical Vehicle
        Vehicle vehicle = vehicleApp.getExistingVehicle();
        if (vehicle == null) {
            vehicle = vehicleRepository.findByVinNumber(vehicleApp.getVinNumber())
                    .orElseGet(() -> {
                        Vehicle newV = new Vehicle();
                        newV.setVinNumber(vehicleApp.getVinNumber());
                        newV.setNumberPlate(vehicleApp.getNumberPlate());
                        newV.setVehicleType(vehicleApp.getVehicleType());
                        newV.setBrand(vehicleApp.getBrand());
                        newV.setModel(vehicleApp.getModel());
                        return vehicleRepository.save(newV);
                    });
        }

        // 2. If ownership transferred, transition old active ownership to TRANSFERRED
        if (vehicleApp.isOwnershipTransferred()) {
            vehicleOwnershipRepository.findByVehicleAndCurrentStatus(vehicle, OwnershipStatus.ACTIVE)
                    .ifPresent(old -> {
                        old.setCurrentStatus(OwnershipStatus.TRANSFERRED);
                        vehicleOwnershipRepository.save(old);
                        recordVehicleOwnershipStatusHistory(old, OwnershipStatus.TRANSFERRED);
                    });
        }

        // 3. Create active VehicleOwnership for this delivery agent
        VehicleOwnership ownership = new VehicleOwnership();
        ownership.setOwner(vehicleApp.getDeliveryAgent());
        ownership.setVehicle(vehicle);
        ownership.setCurrentStatus(OwnershipStatus.ACTIVE);
        VehicleOwnership savedOwnership = vehicleOwnershipRepository.save(ownership);
        recordVehicleOwnershipStatusHistory(savedOwnership, OwnershipStatus.ACTIVE);

        // 4. Promote documents to VehicleOwnershipDocument
        List<VehicleOwnershipDocument> vehicleDocs = vehicleApp.getDocuments().stream()
                .map(doc -> {
                    VehicleOwnershipDocument vDoc = new VehicleOwnershipDocument();
                    vDoc.setVehicleOwnership(savedOwnership);
                    vDoc.setType(doc.getType());
                    vDoc.setUrl(doc.getUrl());
                    vDoc.setStatus(DocumentVerificationStatus.APPROVED);
                    vDoc.setReviewedBy(admin);
                    vDoc.setReviewedAt(Instant.now());
                    return vDoc;
                })
                .toList();

        vehicleOwnershipDocumentRepository.saveAll(vehicleDocs);

        // 5. Update vehicle application status
        vehicleApp.setStatus(ApplicationStatus.APPROVED);
        vehicleApp.setReviewedBy(admin);
        vehicleApp.setReviewedAt(Instant.now());
        applicationVehicleRepository.save(vehicleApp);

        eventPublisher.publishEvent(new VehicleApplicationApprovedEvent(
                vehicleApp.getId(),
                vehicleApp.getDeliveryAgent().getUser().getId(),
                vehicleApp.getDeliveryAgent().getId(),
                vehicleApp.getBrand() + " " + vehicleApp.getModel(),
                adminId,
                Instant.now()
        ));
    }

    @Override
    public void rejectVehicleApplication(UUID vehicleAppId, UUID adminId, String remarks) {
        VehicleApplication vehicleApp = applicationVehicleRepository.findById(vehicleAppId)
                .orElseThrow(() -> new ApplicationNotFoundException("Vehicle application not found: " + vehicleAppId));

        checkApplicationStatus(vehicleApp);

        User admin = loadUser(adminId);
        vehicleApp.setStatus(ApplicationStatus.REJECTED);
        vehicleApp.setReviewedBy(admin);
        vehicleApp.setReviewedAt(Instant.now());
        vehicleApp.setRejectionRemarks(remarks);
        applicationVehicleRepository.save(vehicleApp);

        eventPublisher.publishEvent(new VehicleApplicationRejectedEvent(
                vehicleApp.getId(),
                vehicleApp.getDeliveryAgent().getUser().getId(),
                vehicleApp.getDeliveryAgent().getId(),
                vehicleApp.getBrand() + " " + vehicleApp.getModel(),
                remarks,
                adminId,
                Instant.now()
        ));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private void recordVehicleOwnershipStatusHistory(VehicleOwnership vehicleOwnership, OwnershipStatus newStatus) {
        VehicleOwnershipStatusHistory history = new VehicleOwnershipStatusHistory();
        history.setVehicleOwnership(vehicleOwnership);
        history.setStatus(newStatus);
        vehicleOwnershipStatusHistoryRepository.save(history);
    }

    private void checkApplicationStatus(VehicleApplication application) {
        if (application.getStatus() == ApplicationStatus.APPROVED) {
            throw new BadRequestException("Application is already approved.");
        }

        if (application.getStatus() != ApplicationStatus.SUBMITTED && application.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new ApplicationStateException(
                    "Only SUBMITTED or UNDER_REVIEW applications can be reviewed. Current status: " + application.getStatus());
        }
    }

    /**
     * After updating vehicle details or documents, sync the vehicleComplete flag on the onboard
     * DeliveryAgentApplication (only relevant for Flow 1 / onboarding).
     */
    private void updateOnboardApplicationVehicleComplete(DeliveryAgentApplication onboardApp, VehicleApplication vehicle) {
        boolean allMandatoryDocsPresent = MANDATORY_VEHICLE_DOCUMENT_TYPES.stream()
                .allMatch(type -> applicationVehicleDocumentRepository
                        .existsByApplicationVehicleAndType(vehicle, type));
        boolean vehicleInfoSet = vehicle.getVinNumber() != null && !vehicle.getVinNumber().isBlank()
                && vehicle.getNumberPlate() != null && !vehicle.getNumberPlate().isBlank();
        onboardApp.setVehicleComplete(vehicleInfoSet && allMandatoryDocsPresent);
        deliveryAgentApplicationRepository.save(onboardApp);
    }

    /**
     * Loads a vehicle application for display or read-only access.
     * Works for both Flow 1 (onboarding via application.agent.id) and Flow 2 (standalone via deliveryAgent.user.id).
     */
    private VehicleApplication loadVehicleApplicationForUser(UUID vehicleAppId, UUID userId) {
        return applicationVehicleRepository.findByIdAndOwnerUserId(vehicleAppId, userId)
                .orElseThrow(() -> new ApplicationNotFoundException("Vehicle application not found: " + vehicleAppId));
    }

    private VehicleApplication loadDraftVehicleApplication(UUID vehicleAppId, UUID userId) {
        VehicleApplication app = loadVehicleApplicationForUser(vehicleAppId, userId);

        return loadDraftVehicleApplication(app);
    }

    private VehicleApplication loadDraftVehicleApplication(VehicleApplication vehicleApp) {
        if (vehicleApp.getStatus() != ApplicationStatus.DRAFT) {
            throw new BadRequestException("Vehicle application cannot be edited in its current status: " + vehicleApp.getStatus());
        }
        return vehicleApp;
    }
    
    private DeliveryAgentApplication loadDraftDeliveryAgentApplication(VehicleApplication vehicleApp) {
        DeliveryAgentApplication onboardApp = vehicleApp.getApplication();

        if (onboardApp == null) {
            throw new BadRequestException("Vehicle application is not part of a onboard delivery agent application.");
        }

        if (onboardApp.getStatus() != ApplicationStatus.DRAFT) {
            throw new BadRequestException("Onboard delivery agent application cannot be edited in its current status: " + onboardApp.getStatus());
        }

        return onboardApp;
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private DeliveryAgent loadDeliveryAgentByUserId(UUID userId) {
        User user = loadUser(userId);
        return deliveryAgentRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException(
                        "Only approved delivery agents can perform this operation. No delivery agent profile found for user: " + userId));
    }
}
