package com.quickbite.quickbite.onboarding.service.restaurant;

import com.quickbite.quickbite.allotment.model.AdminAllotment;
import com.quickbite.quickbite.allotment.model.AllotmentReferenceType;
import com.quickbite.quickbite.allotment.service.AdminAllotmentService;
import com.quickbite.quickbite.common.event.restaurantapplication.RestaurantApplicationSubmittedEvent;
import com.quickbite.quickbite.common.event.restaurantapplication.RestaurantApplicationApprovedEvent;
import com.quickbite.quickbite.common.event.restaurantapplication.RestaurantApplicationRejectedEvent;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.common.model.DocumentVerificationStatus;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.onboarding.dto.restaurant.*;
import com.quickbite.quickbite.onboarding.exception.ApplicationNotFoundException;
import com.quickbite.quickbite.onboarding.exception.ApplicationStateException;
import com.quickbite.quickbite.onboarding.model.*;
import com.quickbite.quickbite.onboarding.model.restaurant.*;
import com.quickbite.quickbite.onboarding.repository.*;
import com.quickbite.quickbite.restaurant.model.*;
import com.quickbite.quickbite.restaurant.repository.*;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.AddressRepository;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class RestaurantApplicationServiceImpl implements RestaurantApplicationService, AdminRestaurantApplicationService {

    private static final List<ApplicationStatus> ACTIVE_STATUSES =
            List.of(ApplicationStatus.DRAFT, ApplicationStatus.SUBMITTED, ApplicationStatus.UNDER_REVIEW);

    private static final Set<RestaurantDocumentType> MANDATORY_DOCUMENT_TYPES = EnumSet.of(
            RestaurantDocumentType.FSSAI_LICENSE,
            RestaurantDocumentType.GST,
            RestaurantDocumentType.PAN,
            RestaurantDocumentType.BANK_PROOF
    );

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final RestaurantApplicationRepository applicationRepository;
    private final ApplicationHoursRepository hoursRepository;
    private final ApplicationImageRepository imageRepository;
    private final ApplicationDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantHoursRepository restaurantHoursRepository;
    private final RestaurantImageRepository restaurantImageRepository;
    private final RestaurantDocumentRepository restaurantDocumentRepository;
    private final RestaurantVerificationStatusHistoryRepository statusHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AdminAllotmentService adminAllotmentService;

    public RestaurantApplicationServiceImpl(
            RestaurantApplicationRepository applicationRepository,
            ApplicationHoursRepository hoursRepository,
            ApplicationImageRepository imageRepository,
            ApplicationDocumentRepository documentRepository,
            UserRepository userRepository,
            AddressRepository addressRepository,
            RestaurantRepository restaurantRepository,
            RestaurantHoursRepository restaurantHoursRepository,
            RestaurantImageRepository restaurantImageRepository,
            RestaurantDocumentRepository restaurantDocumentRepository,
            RestaurantVerificationStatusHistoryRepository statusHistoryRepository,
            ApplicationEventPublisher eventPublisher,
            AdminAllotmentService adminAllotmentService) {
        this.applicationRepository = applicationRepository;
        this.hoursRepository = hoursRepository;
        this.imageRepository = imageRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.restaurantRepository = restaurantRepository;
        this.restaurantHoursRepository = restaurantHoursRepository;
        this.restaurantImageRepository = restaurantImageRepository;
        this.restaurantDocumentRepository = restaurantDocumentRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.eventPublisher = eventPublisher;
        this.adminAllotmentService = adminAllotmentService;
    }

    // -------------------------------------------------------------------------
    // Owner operations
    // -------------------------------------------------------------------------

    @Override
    public RestaurantApplicationResponse startApplication(UUID ownerId) {
        User owner = loadUser(ownerId);

        if (applicationRepository.existsByOwnerAndStatusIn(owner, ACTIVE_STATUSES)) {
            throw new ApplicationStateException(
                    "You already have an active application in progress. " +
                            "Complete or withdraw the existing one before starting a new application.");
        }

        RestaurantApplication application = new RestaurantApplication();
        application.setOwner(owner);
        application.setStatus(ApplicationStatus.DRAFT);
        return RestaurantApplicationResponse.from(applicationRepository.save(application));
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantApplicationResponse getCurrentApplication(UUID ownerId) {
        User owner = loadUser(ownerId);
        RestaurantApplication app = applicationRepository
                .findByOwnerAndStatusIn(owner, ACTIVE_STATUSES)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "No active application found. Start a new application first."));
        return RestaurantApplicationResponse.from(app);
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantApplicationResponse getApplication(UUID appId, UUID ownerId) {
        return RestaurantApplicationResponse.from(loadOwnerApplication(appId, ownerId));
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantApplicationDetailsResponse getDetails(UUID appId, UUID ownerId) {
        return RestaurantApplicationDetailsResponse.from(loadOwnerApplication(appId, ownerId));
    }

    @Override
    public RestaurantApplicationDetailsResponse saveDetails(UUID appId, UUID ownerId, RestaurantApplicationDetailsRequest request) {
        RestaurantApplication app = loadDraftApplication(appId, ownerId);
        app.setName(request.name());
        app.setDescription(request.description());
        app.setDetailsComplete(true);
        RestaurantApplication saved = applicationRepository.save(app);
        return RestaurantApplicationDetailsResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantApplicationAddressResponse getAddress(UUID appId, UUID ownerId) {
        return RestaurantApplicationAddressResponse.from(loadOwnerApplication(appId, ownerId));
    }

    @Override
    public RestaurantApplicationAddressResponse saveAddress(UUID appId, UUID ownerId, RestaurantApplicationAddressRequest request) {
        RestaurantApplication app = loadDraftApplication(appId, ownerId);
        app.setAddressStreet(request.street());
        app.setAddressCity(request.city());
        app.setAddressState(request.state());
        app.setAddressCountry(request.country());
        app.setAddressPostalCode(request.postalCode());
        app.setAddressHouseNumber(request.houseNumber());
        app.setAddressBuildingName(request.buildingName());
        app.setAddressLandmark(request.landmark());
        if (request.latitude() != null && request.longitude() != null) {
            Point location = GEOMETRY_FACTORY.createPoint(
                    new Coordinate(request.longitude(), request.latitude()));
            app.setAddressLocation(location);
        }
        app.setAddressComplete(true);
        RestaurantApplication saved = applicationRepository.save(app);
        return RestaurantApplicationAddressResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantApplicationHoursResponse> getHours(UUID appId, UUID ownerId) {
        RestaurantApplication app = loadOwnerApplication(appId, ownerId);
        return hoursRepository.findByApplication(app).stream()
                .map(RestaurantApplicationHoursResponse::from)
                .toList();
    }

    @Override
    public List<RestaurantApplicationHoursResponse> saveHours(UUID appId, UUID ownerId, RestaurantApplicationHoursRequest request) {
        RestaurantApplication app = loadDraftApplication(appId, ownerId);

        // Replace-all strategy: delete existing and re-insert
        hoursRepository.deleteAllByApplication(app);

        List<RestaurantApplicationHours> newHours = request.hours().stream()
                .map(entry -> {
                    RestaurantApplicationHours h = new RestaurantApplicationHours();
                    h.setApplication(app);
                    h.setDayOfWeek(entry.dayOfWeek());
                    h.setOpenTime(entry.openTime());
                    h.setCloseTime(entry.closeTime());
                    return h;
                })
                .toList();
        List<RestaurantApplicationHours> savedHours = hoursRepository.saveAll(newHours);

        app.setHoursComplete(!newHours.isEmpty());
        applicationRepository.save(app);

        return savedHours.stream()
                .map(RestaurantApplicationHoursResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantApplicationImageResponse> getImage(UUID appId, UUID ownerId) {
        RestaurantApplication app = loadOwnerApplication(appId, ownerId);
        return imageRepository.findByApplicationOrderByDisplayOrderAsc(app).stream()
                .map(RestaurantApplicationImageResponse::from)
                .toList();
    }

    @Override
    public RestaurantApplicationImageResponse addImage(UUID appId, UUID ownerId, RestaurantApplicationImageRequest request) {
        RestaurantApplication app = loadDraftApplication(appId, ownerId);

        RestaurantApplicationImage image = new RestaurantApplicationImage();
        image.setApplication(app);
        image.setImageUrl(request.imageUrl());
        image.setDisplayOrder(request.displayOrder());
        RestaurantApplicationImage savedImage = imageRepository.save(image);

        checkAndUpdateImageCompletion(app);
        applicationRepository.save(app);

        return RestaurantApplicationImageResponse.from(savedImage);
    }

    @Override
    public void removeImage(UUID appId, UUID ownerId, UUID imageId) {
        RestaurantApplication app = loadDraftApplication(appId, ownerId);
        RestaurantApplicationImage image = imageRepository.findByIdAndApplication(imageId, app)
                .orElseThrow(() -> new ApplicationNotFoundException("Image not found"));
        imageRepository.delete(image);

        checkAndUpdateImageCompletion(app);
        applicationRepository.save(app);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantApplicationDocumentResponse> getDocuments(UUID appId, UUID ownerId) {
        RestaurantApplication app = loadOwnerApplication(appId, ownerId);
        return documentRepository.findByApplication(app).stream()
                .map(RestaurantApplicationDocumentResponse::from)
                .toList();
    }

    @Override
    public RestaurantApplicationDocumentResponse addDocument(UUID appId, UUID ownerId, RestaurantApplicationDocumentRequest request) {
        RestaurantApplication app = loadDraftApplication(appId, ownerId);

        // Upsert: delete existing entry for this type, then insert fresh
        documentRepository.findByApplicationAndType(app, request.type())
                .ifPresent(documentRepository::delete);

        RestaurantApplicationDocument document = new RestaurantApplicationDocument();
        document.setApplication(app);
        document.setType(request.type());
        document.setUrl(request.url());
        RestaurantApplicationDocument savedDoc = documentRepository.save(document);

        // Check if all mandatory document types are now present
        checkAndUpdateDocumentCompletion(app);
        applicationRepository.save(app);

        return RestaurantApplicationDocumentResponse.from(savedDoc);
    }

    @Override
    public void removeDocument(UUID appId, UUID ownerId, RestaurantDocumentType type) {
        RestaurantApplication app = loadDraftApplication(appId, ownerId);
        RestaurantApplicationDocument doc = documentRepository.findByApplicationAndType(app, type)
                .orElseThrow(() -> new ApplicationNotFoundException("Document of type "
                        + type + " not found"));
        documentRepository.delete(doc);
        checkAndUpdateDocumentCompletion(app);
        applicationRepository.save(app);
    }

    @Override
    public RestaurantApplicationResponse submitApplication(UUID appId, UUID ownerId) {
        RestaurantApplication app = loadDraftApplication(appId, ownerId);

        // Validate all steps are complete
        if (!app.isDetailsComplete() || !app.isAddressComplete() || !app.isHoursComplete()
                || !app.isImagesComplete() || !app.isDocumentsComplete()) {
            throw new ApplicationStateException(
                    "Application is incomplete. Please complete all steps before submitting: " +
                            "details, address, hours, images, and required documents.");
        }

        app.setStatus(ApplicationStatus.SUBMITTED);
        RestaurantApplication savedApp = applicationRepository.save(app);

        // Allot request to workload-balanced admins
        List<AdminAllotment> adminAllotments = adminAllotmentService.allot(savedApp.getId(), AllotmentReferenceType.RESTAURANT_APPLICATION);

        recordStatusHistory(savedApp, null, RestaurantVerificationStatus.PENDING, "Application submitted for review");

        // Registered for AFTER_COMMIT — RestaurantApplicationKafkaEventPublisher sends to Kafka
        // only after this transaction has durably committed.
        eventPublisher.publishEvent(new RestaurantApplicationSubmittedEvent(
                savedApp.getId(),
                savedApp.getOwner().getId(),
                savedApp.getOwner().getEmail(),
                savedApp.getOwner().getName(),
                savedApp.getName(),
                adminAllotments.stream()
                        .map(a -> a.getAdmin().getId())
                        .toList(),
                Instant.now()));

        return RestaurantApplicationResponse.from(savedApp);
    }

    @Override
    public RestaurantApplicationResponse reopenApplication(UUID appId, UUID ownerId) {
        RestaurantApplication app = loadOwnerApplication(appId, ownerId);

        if (app.getStatus() != ApplicationStatus.REJECTED) {
            throw new ApplicationStateException(
                    "Only REJECTED applications can be reopened. Current status: " + app.getStatus());
        }

        app.setStatus(ApplicationStatus.DRAFT);
        app.setRejectionRemarks(null);
        return RestaurantApplicationResponse.from(applicationRepository.save(app));
    }

    // -------------------------------------------------------------------------
    // Admin operations
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public CursorPage<RestaurantApplicationSummaryResponse> listApplications(ApplicationStatus status, UUID cursor, int size) {
        int pageSize = Math.clamp(size, 1, 100);
        List<RestaurantApplication> fetched =
                applicationRepository.findByStatusWithCursor(status, cursor, Limit.of(pageSize + 1));
        return CursorPage.of(
                fetched.stream().map(RestaurantApplicationSummaryResponse::from).toList(),
                pageSize,
                RestaurantApplicationSummaryResponse::id);
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantApplicationResponse getApplicationAsAdmin(UUID appId) {
        RestaurantApplication app = applicationRepository.findById(appId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + appId));
        return RestaurantApplicationResponse.from(app);
    }

    @Override
    public void approveApplication(UUID appId, UUID adminId) {
        RestaurantApplication app = applicationRepository.findById(appId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + appId));

        checkApplicationStatus(app);

        User admin = loadUser(adminId);
        Restaurant restaurant = promoteToRestaurant(app, admin);

        app.setRestaurant(restaurant);
        app.setStatus(ApplicationStatus.APPROVED);
        app.setReviewedBy(admin);
        Instant approvedAt = Instant.now();
        app.setReviewedAt(approvedAt);
        applicationRepository.save(app);

        // Fires after this @Transactional commits — restaurant row is guaranteed visible
        eventPublisher.publishEvent(new RestaurantApplicationApprovedEvent(
                app.getId(),
                restaurant.getId(),
                app.getOwner().getId(),
                app.getOwner().getEmail(),
                app.getOwner().getName(),
                restaurant.getName(),
                admin.getId(),
                approvedAt));
    }

    @Override
    public void rejectApplication(UUID appId, UUID adminId, String remarks) {
        RestaurantApplication app = applicationRepository.findById(appId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + appId));

        checkApplicationStatus(app);

        User admin = loadUser(adminId);
        app.setStatus(ApplicationStatus.REJECTED);
        app.setRejectionRemarks(remarks);
        app.setReviewedBy(admin);
        Instant rejectedAt = Instant.now();
        app.setReviewedAt(rejectedAt);
        applicationRepository.save(app);

        recordStatusHistory(app, admin, RestaurantVerificationStatus.REJECTED, remarks);

        // Fires after this @Transactional commits
        eventPublisher.publishEvent(new RestaurantApplicationRejectedEvent(
                app.getId(),
                app.getOwner().getId(),
                app.getOwner().getEmail(),
                app.getOwner().getName(),
                app.getName(),
                admin.getId(),
                remarks,
                rejectedAt));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Promotes an approved application into a fully-valid Restaurant aggregate.
     * Creates Address, Restaurant, RestaurantHours, RestaurantImages,
     * RestaurantDocuments, and a RestaurantVerificationStatusHistory entry.
     */
    private Restaurant promoteToRestaurant(RestaurantApplication app, User admin) {
        // 1. Create Address from embedded application fields
        Address address = createAddress(app);
        address = addressRepository.save(address);

        // 2. Create Restaurant
        Restaurant restaurant = new Restaurant();
        restaurant.setOwner(app.getOwner());
        restaurant.setName(app.getName());
        restaurant.setDescription(app.getDescription());
        restaurant.setAddress(address);
        restaurant.setTotalRating(0L);
        restaurant.setAvgRating(null);
        restaurant.setClosed(false);
        restaurant.setCurrentStatus(RestaurantVerificationStatus.APPROVED);
        final Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        // 3. Migrate operating hours
        List<RestaurantHours> hours = app.getHours().stream()
                .map(h -> {
                    RestaurantHours rh = new RestaurantHours();
                    rh.setRestaurant(savedRestaurant);
                    rh.setDayOfWeek(h.getDayOfWeek());
                    rh.setOpenTime(h.getOpenTime());
                    rh.setCloseTime(h.getCloseTime());
                    return rh;
                })
                .toList();
        restaurantHoursRepository.saveAll(hours);

        // 4. Migrate images
        List<RestaurantImage> images = app.getImages().stream()
                .map(img -> {
                    RestaurantImage ri = new RestaurantImage();
                    ri.setRestaurant(savedRestaurant);
                    ri.setImageUrl(img.getImageUrl());
                    ri.setDisplayOrder(img.getDisplayOrder());
                    return ri;
                })
                .toList();
        restaurantImageRepository.saveAll(images);

        // 5. Migrate documents (mark all as APPROVED by admin)
        List<RestaurantDocument> documents = app.getDocuments().stream()
                .map(doc -> {
                    RestaurantDocument rd = new RestaurantDocument();
                    rd.setRestaurant(savedRestaurant);
                    rd.setType(doc.getType());
                    rd.setUrl(doc.getUrl());
                    rd.setStatus(DocumentVerificationStatus.APPROVED);
                    rd.setReviewedBy(admin);
                    rd.setReviewedAt(Instant.now());
                    return rd;
                })
                .toList();
        restaurantDocumentRepository.saveAll(documents);

        // 6. Record status history
        recordStatusHistory(app, admin, RestaurantVerificationStatus.APPROVED, "Application approved by admin");

        return savedRestaurant;
    }

    private void recordStatusHistory(RestaurantApplication application, User admin, RestaurantVerificationStatus status, String remarks) {
        RestaurantVerificationStatusHistory history = new RestaurantVerificationStatusHistory();
        history.setApplication(application);
        history.setReviewedBy(admin);
        history.setStatus(status);
        history.setRemarks(remarks);
        statusHistoryRepository.save(history);
    }

    private static Address createAddress(RestaurantApplication app) {
        Address address = new Address();
        address.setUser(app.getOwner());
        address.setLabel("Restaurant Address");
        address.setStreet(app.getAddressStreet());
        address.setCity(app.getAddressCity());
        address.setState(app.getAddressState());
        address.setCountry(app.getAddressCountry());
        address.setPostalCode(app.getAddressPostalCode());
        address.setHouseNumber(app.getAddressHouseNumber());
        address.setBuildingName(app.getAddressBuildingName());
        address.setLandmark(app.getAddressLandmark());
        address.setLocation(app.getAddressLocation());
        address.setIsDefault(false);
        return address;
    }

    private void checkApplicationStatus(RestaurantApplication app) throws ApplicationStateException, BadRequestException {
        if (app.getStatus() == ApplicationStatus.APPROVED) {
            throw new BadRequestException("Approved applications cannot be reviewed.");
        }

        if (app.getStatus() != ApplicationStatus.SUBMITTED && app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new ApplicationStateException(
                    "Only SUBMITTED or UNDER_REVIEW applications can be reviewed. Current status: " + app.getStatus());
        }
    }

    private void checkAndUpdateDocumentCompletion(RestaurantApplication app) {
        boolean allMandatoryPresent = MANDATORY_DOCUMENT_TYPES.stream()
                .allMatch(type -> documentRepository
                        .existsByApplicationAndType(app, type));
        app.setDocumentsComplete(allMandatoryPresent);
    }

    private void checkAndUpdateImageCompletion(RestaurantApplication app) {
        long imageCount = imageRepository.countByApplication(app);
        app.setImagesComplete(imageCount >= 1);
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    /**
     * Loads an application by ID belonging to a specific owner, regardless of status.
     */
    private RestaurantApplication loadOwnerApplication(UUID appId, UUID ownerId) {
        User owner = loadUser(ownerId);
        return applicationRepository.findByIdAndOwner(appId, owner)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found or does not belong to this account: " + appId));
    }

    /**
     * Loads an application that must be in DRAFT status for the given owner.
     * Throws {@link ApplicationStateException} if the application is not editable.
     */
    private RestaurantApplication loadDraftApplication(UUID appId, UUID ownerId) {
        RestaurantApplication app = loadOwnerApplication(appId, ownerId);
        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new ApplicationStateException(
                    "Application can only be edited when in DRAFT status. Current status: " + app.getStatus());
        }
        return app;
    }
}
