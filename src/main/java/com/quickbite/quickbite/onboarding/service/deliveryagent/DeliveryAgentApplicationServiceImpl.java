package com.quickbite.quickbite.onboarding.service.deliveryagent;

import com.quickbite.quickbite.allotment.model.AdminAllotment;
import com.quickbite.quickbite.allotment.model.AllotmentReferenceType;
import com.quickbite.quickbite.allotment.service.AdminAllotmentService;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.common.event.deliveryagentapplication.DeliveryAgentApplicationApprovedEvent;
import com.quickbite.quickbite.common.event.deliveryagentapplication.DeliveryAgentApplicationRejectedEvent;
import com.quickbite.quickbite.common.event.deliveryagentapplication.DeliveryAgentApplicationSubmittedEvent;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.common.model.DocumentVerificationStatus;
import com.quickbite.quickbite.delivery.model.*;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentDocumentRepository;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentVerificationHistoryRepository;
import com.quickbite.quickbite.onboarding.dto.deliveryagent.DeliveryAgentApplicationDocumentRequest;
import com.quickbite.quickbite.onboarding.dto.deliveryagent.DeliveryAgentApplicationDocumentResponse;
import com.quickbite.quickbite.onboarding.dto.deliveryagent.DeliveryAgentApplicationResponse;
import com.quickbite.quickbite.onboarding.dto.deliveryagent.DeliveryAgentApplicationSummaryResponse;
import com.quickbite.quickbite.onboarding.dto.vehicle.VehicleApplicationResponse;
import com.quickbite.quickbite.onboarding.exception.ApplicationNotFoundException;
import com.quickbite.quickbite.onboarding.exception.ApplicationStateException;
import com.quickbite.quickbite.onboarding.model.*;
import com.quickbite.quickbite.onboarding.model.deliveryagent.DeliveryAgentApplication;
import com.quickbite.quickbite.onboarding.model.deliveryagent.DeliveryAgentApplicationDocument;
import com.quickbite.quickbite.onboarding.model.vehicle.VehicleApplication;
import com.quickbite.quickbite.onboarding.model.deliveryagent.DeliveryAgentVerificationHistory;
import com.quickbite.quickbite.onboarding.repository.*;
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
public class DeliveryAgentApplicationServiceImpl implements DeliveryAgentApplicationService, AdminDeliveryAgentApplicationService {

    private final static List<DeliveryAgentDocumentType> MANDATORY_DOCUMENT_TYPES =
            List.of(DeliveryAgentDocumentType.AADHAR, DeliveryAgentDocumentType.DRIVING_LICENSE);

    private final DeliveryAgentApplicationRepository applicationRepository;
    private final DeliveryAgentApplicationDocumentRepository applicationDocumentRepository;
    private final DeliveryAgentApplicationVehicleRepository applicationVehicleRepository;
    private final UserRepository userRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final DeliveryAgentDocumentRepository deliveryAgentDocumentRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleOwnershipRepository vehicleOwnershipRepository;
    private final VehicleOwnershipDocumentRepository vehicleOwnershipDocumentRepository;
    private final AdminAllotmentService adminAllotmentService;
    private final DeliveryAgentVerificationHistoryRepository deliveryAgentVerificationHistoryRepository;
    private final VehicleOwnershipStatusHistoryRepository vehicleOwnershipStatusHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DeliveryAgentApplicationServiceImpl(
            DeliveryAgentApplicationRepository applicationRepository,
            DeliveryAgentApplicationDocumentRepository applicationDocumentRepository,
            DeliveryAgentApplicationVehicleRepository applicationVehicleRepository,
            UserRepository userRepository,
            DeliveryAgentRepository deliveryAgentRepository,
            DeliveryAgentDocumentRepository deliveryAgentDocumentRepository,
            VehicleRepository vehicleRepository,
            VehicleOwnershipRepository vehicleOwnershipRepository,
            VehicleOwnershipDocumentRepository vehicleOwnershipDocumentRepository,
            AdminAllotmentService adminAllotmentService,
            DeliveryAgentVerificationHistoryRepository deliveryAgentVerificationHistoryRepository,
            VehicleOwnershipStatusHistoryRepository vehicleOwnershipStatusHistoryRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.applicationRepository = applicationRepository;
        this.applicationDocumentRepository = applicationDocumentRepository;
        this.applicationVehicleRepository = applicationVehicleRepository;
        this.userRepository = userRepository;
        this.deliveryAgentRepository = deliveryAgentRepository;
        this.deliveryAgentDocumentRepository = deliveryAgentDocumentRepository;
        this.vehicleRepository = vehicleRepository;
        this.vehicleOwnershipRepository = vehicleOwnershipRepository;
        this.vehicleOwnershipDocumentRepository = vehicleOwnershipDocumentRepository;
        this.adminAllotmentService = adminAllotmentService;
        this.deliveryAgentVerificationHistoryRepository = deliveryAgentVerificationHistoryRepository;
        this.vehicleOwnershipStatusHistoryRepository = vehicleOwnershipStatusHistoryRepository;
        this.eventPublisher = eventPublisher;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Driver Multi-step Onboarding Wizard
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public DeliveryAgentApplicationResponse startApplication(UUID agentUserId) {
        User agent = loadUser(agentUserId);

        if (deliveryAgentRepository.existsByUser(agent)) {
            throw new BadRequestException("You are already an approved delivery agent. " +
                    "To add a new vehicle, please submit a standalone vehicle application.");
        }

        Optional<DeliveryAgentApplication> existingApp = applicationRepository
                .findByAgentAndStatusIn(
                        agent,
                        List.of(ApplicationStatus.DRAFT, ApplicationStatus.SUBMITTED,
                                ApplicationStatus.UNDER_REVIEW, ApplicationStatus.APPROVED)
                );

        if (existingApp.isPresent()) {
            DeliveryAgentApplication app = existingApp.get();
            if (app.getStatus() == ApplicationStatus.APPROVED) {
                throw new BadRequestException("Your delivery agent application has already been approved.");
            }
            return DeliveryAgentApplicationResponse.from(app);
        }

        DeliveryAgentApplication application = new DeliveryAgentApplication();
        application.setAgent(agent);
        application.setStatus(ApplicationStatus.DRAFT);
        return DeliveryAgentApplicationResponse.from(applicationRepository.save(application));
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAgentApplicationResponse getCurrentApplication(UUID agentUserId) {
        User agent = loadUser(agentUserId);
        DeliveryAgentApplication application = applicationRepository
                .findByAgentAndStatusIn(
                        agent,
                        List.of(ApplicationStatus.DRAFT, ApplicationStatus.SUBMITTED,
                                ApplicationStatus.UNDER_REVIEW, ApplicationStatus.REJECTED)
                )
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "No active delivery agent application found"));

        return DeliveryAgentApplicationResponse.from(application);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAgentApplicationResponse getApplication(UUID appId, UUID agentUserId) {
        return DeliveryAgentApplicationResponse.from(loadOwnerApplication(appId, agentUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAgentApplicationDocumentResponse> getDocument(UUID appId, UUID agentUserId) {
        DeliveryAgentApplication application = loadOwnerApplication(appId, agentUserId);
        return applicationDocumentRepository.findByApplication(application).stream()
                .map(DeliveryAgentApplicationDocumentResponse::from)
                .toList();
    }

    @Override
    public DeliveryAgentApplicationDocumentResponse addDocument(UUID appId, UUID agentUserId, DeliveryAgentApplicationDocumentRequest request) {
        DeliveryAgentApplication application = loadDraftApplication(appId, agentUserId);
        applicationDocumentRepository.findByApplicationAndType(application, request.type())
                .ifPresent(applicationDocumentRepository::delete);
        DeliveryAgentApplicationDocument document = new DeliveryAgentApplicationDocument();
        document.setApplication(application);
        document.setType(request.type());
        document.setUrl(request.url());
        DeliveryAgentApplicationDocument savedDoc = applicationDocumentRepository.save(document);
        checkAndUpdateDocumentsComplete(application);
        applicationRepository.save(application);
        return DeliveryAgentApplicationDocumentResponse.from(savedDoc);
    }

    @Override
    public void removeDocument(UUID appId, UUID agentUserId, DeliveryAgentDocumentType type) {
        DeliveryAgentApplication application = loadDraftApplication(appId, agentUserId);
        DeliveryAgentApplicationDocument doc = applicationDocumentRepository
                .findByApplicationAndType(application, type)
                .orElseThrow(() -> new BadRequestException("No document of type " + type + " found for this application."));
        applicationDocumentRepository.delete(doc);
        checkAndUpdateDocumentsComplete(application);
        applicationRepository.save(application);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleApplicationResponse getCurrentVehicleApplication(UUID id, UUID agentUserId) {
        DeliveryAgentApplication application = loadOwnerApplication(id, agentUserId);
        VehicleApplication vehicleApp = applicationVehicleRepository
                .findByApplication(application)
                .orElseThrow(() -> new ResourceNotFoundException("No vehicle associated with this application."));
        return VehicleApplicationResponse.from(vehicleApp);
    }

    @Override
    public VehicleApplicationResponse startVehicleApplication(UUID id, UUID agentUserId) {
        DeliveryAgentApplication application = loadDraftApplication(id, agentUserId);
        VehicleApplication vehicleApp = applicationVehicleRepository
                .findByApplication(application)
                .orElseGet(() -> {
                    VehicleApplication newVehicle = new VehicleApplication();
                    newVehicle.setApplication(application);
                    return applicationVehicleRepository.save(newVehicle);
                });
        return VehicleApplicationResponse.from(vehicleApp);
    }

    @Override
    public DeliveryAgentApplicationResponse submitApplication(UUID appId, UUID agentUserId) {
        DeliveryAgentApplication application = loadDraftApplication(appId, agentUserId);

        if (!application.isDocumentsComplete()) {
            throw new BadRequestException("Please ensure all mandatory personal documents are uploaded before submitting the application.");
        }

        if (!application.isVehicleComplete()) {
            throw new BadRequestException("Please ensure all mandatory vehicle documents are uploaded and vehicle details are complete before submitting the application.");
        }

        application.setStatus(ApplicationStatus.SUBMITTED);
        DeliveryAgentApplication saved = applicationRepository.save(application);

        // Keep associated child vehicle application status aligned
        applicationVehicleRepository.findByApplication(saved)
                .ifPresent(v -> {
                    v.setStatus(ApplicationStatus.SUBMITTED);
                    applicationVehicleRepository.save(v);
                });

        recordDeliveryAgentVerificationHistory(saved, DeliveryAgentVerificationStatus.PENDING, null, "Application submitted");

        // Allot review task to admins
        List<AdminAllotment> allottedAdmins = adminAllotmentService.allot(saved.getId(), AllotmentReferenceType.DELIVERY_AGENT);

        eventPublisher.publishEvent(new DeliveryAgentApplicationSubmittedEvent(
                saved.getId(),
                saved.getAgent().getId(),
                allottedAdmins.stream()
                        .map((aa) -> aa.getAdmin().getId())
                        .toList(),
                Instant.now()
        ));

        return DeliveryAgentApplicationResponse.from(saved);
    }

    @Override
    public DeliveryAgentApplicationResponse reopenApplication(UUID appId, UUID agentUserId) {
        DeliveryAgentApplication application = loadOwnerApplication(appId, agentUserId);

        if (application.getStatus() != ApplicationStatus.REJECTED) {
            throw new BadRequestException("Only REJECTED applications can be reopened. Current status: " + application.getStatus());
        }

        application.setStatus(ApplicationStatus.DRAFT);
        DeliveryAgentApplication saved = applicationRepository.save(application);

        // Reset associated child vehicle application status to DRAFT so it can be edited
        applicationVehicleRepository.findByApplication(saved)
                .ifPresent(v -> {
                    v.setStatus(ApplicationStatus.DRAFT);
                    applicationVehicleRepository.save(v);
                });

        return DeliveryAgentApplicationResponse.from(saved);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Admin Review Operations
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CursorPage<DeliveryAgentApplicationSummaryResponse> listApplications(ApplicationStatus status, UUID cursor, int size) {
        int pageSize = Math.clamp(size, 1, 100);
        List<DeliveryAgentApplication> apps = applicationRepository.findWithCursor(status, cursor, Limit.of(pageSize + 1));

        return CursorPage.of(
                apps.stream().map(DeliveryAgentApplicationSummaryResponse::from).toList(),
                pageSize,
                DeliveryAgentApplicationSummaryResponse::id
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAgentApplicationResponse getApplicationAsAdmin(UUID appId) {
        DeliveryAgentApplication app = applicationRepository.findById(appId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + appId));
        return DeliveryAgentApplicationResponse.from(app);
    }

    @Override
    public void approveApplication(UUID appId, UUID adminId) {
        DeliveryAgentApplication application = applicationRepository.findById(appId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + appId));

        checkApplicationStatus(application);

        User admin = loadUser(adminId);
        User agentUser = application.getAgent();

        // 1. Activate User account
        agentUser.setActive(true);
        userRepository.save(agentUser);

        // 2. Create DeliveryAgent profile
        DeliveryAgent deliveryAgent = deliveryAgentRepository.findByUser(agentUser).orElseGet(() -> {
            DeliveryAgent newAgent = new DeliveryAgent();
            newAgent.setUser(agentUser);
            newAgent.setCurrentStatus(DeliveryAgentVerificationStatus.APPROVED);
            newAgent.setAvailable(false);
            return deliveryAgentRepository.save(newAgent);
        });

        // 3. Promote / Resolve Vehicle
        VehicleApplication appVehicle = applicationVehicleRepository
                .findByApplication(application)
                .orElseThrow(() -> new BadRequestException("No vehicle associated with this application."));
        Vehicle vehicle = appVehicle.getExistingVehicle();
        if (vehicle == null) {
            vehicle = vehicleRepository.findByVinNumber(appVehicle.getVinNumber())
                    .orElseGet(() -> {
                        Vehicle newV = new Vehicle();
                        newV.setVinNumber(appVehicle.getVinNumber());
                        newV.setNumberPlate(appVehicle.getNumberPlate());
                        newV.setVehicleType(appVehicle.getVehicleType());
                        newV.setBrand(appVehicle.getBrand());
                        newV.setModel(appVehicle.getModel());
                        return vehicleRepository.save(newV);
                    });
        }

        // 4. Handle Vehicle Transfer
        if (appVehicle.isOwnershipTransferred()) {
            vehicleOwnershipRepository.findByVehicleAndCurrentStatus(vehicle, OwnershipStatus.ACTIVE)
                    .ifPresent(oldOwnership -> {
                        oldOwnership.setCurrentStatus(OwnershipStatus.TRANSFERRED);
                        vehicleOwnershipRepository.save(oldOwnership);
                        recordVehicleOwnershipStatusHistory(oldOwnership, OwnershipStatus.TRANSFERRED);
                    });
        }

        // 5. Create VehicleOwnership
        VehicleOwnership ownership = new VehicleOwnership();
        ownership.setOwner(deliveryAgent);
        ownership.setVehicle(vehicle);
        ownership.setCurrentStatus(OwnershipStatus.ACTIVE);
        VehicleOwnership savedOwnership = vehicleOwnershipRepository.save(ownership);
        recordVehicleOwnershipStatusHistory(savedOwnership, OwnershipStatus.ACTIVE);

        // 6. Promote Vehicle Documents
        List<VehicleOwnershipDocument> vehicleDocs = appVehicle.getDocuments().stream()
                .map(doc -> {
                    VehicleOwnershipDocument vDoc = new VehicleOwnershipDocument();
                    vDoc.setVehicleOwnership(savedOwnership);
                    vDoc.setType(doc.getType());
                    vDoc.setUrl(doc.getUrl());
                    vDoc.setStatus(DocumentVerificationStatus.APPROVED);
                    vDoc.setReviewedBy(admin);
                    vDoc.setReviewedAt(Instant.now());
                    return vDoc;
                }).toList();
        vehicleOwnershipDocumentRepository.saveAll(vehicleDocs);


        // 7. Promote Personal Identity Documents
        List<DeliveryAgentDocument> personalDocs = application.getDocuments().stream()
                .map(doc -> {
                    DeliveryAgentDocument dDoc = new DeliveryAgentDocument();
                    dDoc.setDeliveryAgent(deliveryAgent);
                    dDoc.setType(doc.getType());
                    dDoc.setUrl(doc.getUrl());
                    dDoc.setStatus(DocumentVerificationStatus.APPROVED);
                    dDoc.setReviewedBy(admin);
                    dDoc.setReviewedAt(Instant.now());
                    return dDoc;
                }).toList();
        deliveryAgentDocumentRepository.saveAll(personalDocs);

        // 8. Set active vehicle on DeliveryAgent and link application
        deliveryAgent.setCurrentVehicle(vehicle);
        deliveryAgentRepository.save(deliveryAgent);

        appVehicle.setStatus(ApplicationStatus.APPROVED);
        appVehicle.setReviewedBy(admin);
        appVehicle.setReviewedAt(Instant.now());
        applicationVehicleRepository.save(appVehicle);

        application.setStatus(ApplicationStatus.APPROVED);
        application.setReviewedBy(admin);
        application.setReviewedAt(Instant.now());
        application.setDeliveryAgent(deliveryAgent);
        applicationRepository.save(application);

        recordDeliveryAgentVerificationHistory(application, DeliveryAgentVerificationStatus.APPROVED, admin, "Application approved");

        eventPublisher.publishEvent(new DeliveryAgentApplicationApprovedEvent(
                application.getId(),
                deliveryAgent.getId(),
                application.getAgent().getId(),
                admin.getId(),
                Instant.now()
        ));
    }

    @Override
    public void rejectApplication(UUID appId, UUID adminId, String remarks) {
        DeliveryAgentApplication application = applicationRepository.findById(appId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + appId));

        checkApplicationStatus(application);

        User admin = loadUser(adminId);
        application.setStatus(ApplicationStatus.REJECTED);
        application.setReviewedBy(admin);
        application.setReviewedAt(Instant.now());
        application.setRejectionRemarks(remarks);
        applicationRepository.save(application);

        // Keep associated child vehicle application status aligned
        applicationVehicleRepository.findByApplication(application)
                .ifPresent(v -> {
                    v.setStatus(ApplicationStatus.REJECTED);
                    v.setReviewedBy(admin);
                    v.setReviewedAt(Instant.now());
                    v.setRejectionRemarks(remarks);
                    applicationVehicleRepository.save(v);
                });

        recordDeliveryAgentVerificationHistory(application, DeliveryAgentVerificationStatus.REJECTED, admin, remarks);

        eventPublisher.publishEvent(new DeliveryAgentApplicationRejectedEvent(
                application.getId(),
                application.getAgent().getId(),
                admin.getId(),
                remarks,
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

    private void recordDeliveryAgentVerificationHistory(DeliveryAgentApplication application, DeliveryAgentVerificationStatus newStatus, User admin, String remarks) {
        DeliveryAgentVerificationHistory history = new DeliveryAgentVerificationHistory();
        history.setApplication(application);
        history.setStatus(newStatus);
        history.setReviewedBy(admin);
        history.setRemarks(remarks);
        deliveryAgentVerificationHistoryRepository.save(history);
    }

    private void checkApplicationStatus(DeliveryAgentApplication application) throws ApplicationStateException, BadRequestException {
        if (application.getStatus() == ApplicationStatus.APPROVED) {
            throw new BadRequestException("Application is already approved.");
        }

        if (application.getStatus() != ApplicationStatus.SUBMITTED && application.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new ApplicationStateException(
                    "Only SUBMITTED or UNDER_REVIEW applications can be reviewed. Current status: " + application.getStatus());
        }
    }

    private void checkAndUpdateDocumentsComplete(DeliveryAgentApplication application) {
        boolean allMandatoryDocsPresent = MANDATORY_DOCUMENT_TYPES.stream()
                .allMatch(type -> applicationDocumentRepository
                        .existsByApplicationAndType(application, type));
        application.setDocumentsComplete(allMandatoryDocsPresent);
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private DeliveryAgentApplication loadOwnerApplication(UUID appId, UUID agentUserId) {
        User agent = loadUser(agentUserId);
        return applicationRepository.findByIdAndAgent(appId, agent)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + appId));
    }

    private DeliveryAgentApplication loadDraftApplication(UUID appId, UUID agentUserId) {
        DeliveryAgentApplication app = loadOwnerApplication(appId, agentUserId);

        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new BadRequestException("Application can only be edited when in DRAFT status. Current status: " + app.getStatus());
        }

        return app;
    }
}
