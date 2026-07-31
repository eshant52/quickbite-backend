package com.quickbite.quickbite.onboarding.service;

import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.common.model.DocumentVerificationStatus;
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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class RestaurantApplicationServiceImpl implements RestaurantApplicationService {

    /** Statuses that indicate an owner already has an active application in progress. */
    private static final List<ApplicationStatus> ACTIVE_STATUSES =
            List.of(ApplicationStatus.DRAFT, ApplicationStatus.SUBMITTED, ApplicationStatus.UNDER_REVIEW);

    /** Document types that MUST be uploaded before an application can be submitted. */
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
            RestaurantVerificationStatusHistoryRepository statusHistoryRepository) {
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
    }

    // -------------------------------------------------------------------------
    // Owner operations
    // -------------------------------------------------------------------------

    @Override
    public ApplicationResponse startApplication(UUID ownerId) {
        User owner = loadUser(ownerId);

        if (applicationRepository.existsByOwnerAndStatusIn(owner, ACTIVE_STATUSES)) {
            throw new ApplicationStateException(
                    "You already have an active application in progress. " +
                    "Complete or withdraw the existing one before starting a new application.");
        }

        RestaurantApplication application = new RestaurantApplication();
        application.setOwner(owner);
        application.setStatus(ApplicationStatus.DRAFT);
        return ApplicationResponse.from(applicationRepository.save(application));
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getCurrentApplication(UUID ownerId) {
        User owner = loadUser(ownerId);
        RestaurantApplication app = applicationRepository
                .findByOwnerAndStatusIn(owner, ACTIVE_STATUSES)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "No active application found. Start a new application first."));
        return ApplicationResponse.from(app);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(UUID appId, UUID ownerId) {
        return ApplicationResponse.from(loadOwnerApplication(appId, ownerId));
    }

    @Override
    public ApplicationResponse saveDetails(UUID appId, UUID ownerId, ApplicationDetailsRequest request) {
        RestaurantApplication app = loadDraftApplication(appId, ownerId);
        app.setName(request.name());
        app.setDescription(request.description());
        app.setDetailsComplete(true);
        return ApplicationResponse.from(applicationRepository.save(app));
    }

    @Override
    public ApplicationResponse saveAddress(UUID appId, UUID ownerId, ApplicationAddressRequest request) {
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
        return ApplicationResponse.from(applicationRepository.save(app));
    }

    @Override
    public ApplicationResponse saveHours(UUID appId, UUID ownerId, ApplicationHoursRequest request) {
        RestaurantApplication app = loadDraftApplication(appId, ownerId);

        // Replace-all strategy: delete existing and re-insert
        hoursRepository.deleteAllByApplication(app);

        List<ApplicationHours> newHours = request.hours().stream()
                .map(entry -> {
                    ApplicationHours h = new ApplicationHours();
                    h.setApplication(app);
                    h.setDayOfWeek(entry.dayOfWeek());
                    h.setOpenTime(entry.openTime());
                    h.setCloseTime(entry.closeTime());
                    return h;
                })
                .toList();
        hoursRepository.saveAll(newHours);

        app.setHoursComplete(!newHours.isEmpty());
        return ApplicationResponse.from(applicationRepository.save(app));
    }

    @Override
    public ApplicationResponse addImage(UUID appId, UUID ownerId, ApplicationImageRequest request) {
        RestaurantApplication app = loadDraftApplication(appId, ownerId);

        ApplicationImage image = new ApplicationImage();
        image.setApplication(app);
        image.setImageUrl(request.imageUrl());
        image.setDisplayOrder(request.displayOrder());
        imageRepository.save(image);

        long imageCount = imageRepository.countByApplication(app);
        app.setImagesComplete(imageCount >= 1);
        return ApplicationResponse.from(applicationRepository.save(app));
    }

    @Override
    public void removeImage(UUID appId, UUID ownerId, UUID imageId) {
        RestaurantApplication app = loadDraftApplication(appId, ownerId);
        ApplicationImage image = imageRepository.findByIdAndApplication(imageId, app)
                .orElseThrow(() -> new ApplicationNotFoundException("Image not found"));
        imageRepository.delete(image);

        long remaining = imageRepository.countByApplication(app);
        app.setImagesComplete(remaining >= 1);
        applicationRepository.save(app);
    }

    @Override
    public ApplicationResponse addDocument(UUID appId, UUID ownerId, ApplicationDocumentRequest request) {
        RestaurantApplication app = loadDraftApplication(appId, ownerId);

        // Upsert: delete existing entry for this type, then insert fresh
        documentRepository.findByApplicationAndType(app, request.type())
                .ifPresent(documentRepository::delete);

        ApplicationDocument document = new ApplicationDocument();
        document.setApplication(app);
        document.setType(request.type());
        document.setUrl(request.url());
        documentRepository.save(document);

        // Check if all mandatory document types are now present
        boolean allMandatoryPresent = MANDATORY_DOCUMENT_TYPES.stream()
                .allMatch(type -> documentRepository.existsByApplicationAndType(app, type));
        app.setDocumentsComplete(allMandatoryPresent);
        return ApplicationResponse.from(applicationRepository.save(app));
    }

    @Override
    public void removeDocument(UUID appId, UUID ownerId, UUID documentId) {
        RestaurantApplication app = loadDraftApplication(appId, ownerId);
        ApplicationDocument document = documentRepository.findByIdAndApplication(documentId, app)
                .orElseThrow(() -> new ApplicationNotFoundException("Document not found"));
        documentRepository.delete(document);

        boolean allMandatoryPresent = MANDATORY_DOCUMENT_TYPES.stream()
                .allMatch(type -> documentRepository.existsByApplicationAndType(app, type));
        app.setDocumentsComplete(allMandatoryPresent);
        applicationRepository.save(app);
    }

    @Override
    public ApplicationResponse submitApplication(UUID appId, UUID ownerId) {
        RestaurantApplication app = loadDraftApplication(appId, ownerId);

        // Validate all steps are complete
        if (!app.isDetailsComplete() || !app.isAddressComplete() || !app.isHoursComplete()
                || !app.isImagesComplete() || !app.isDocumentsComplete()) {
            throw new ApplicationStateException(
                    "Application is incomplete. Please complete all steps before submitting: " +
                    "details, address, hours, images, and required documents.");
        }

        app.setStatus(ApplicationStatus.SUBMITTED);
        // TODO: publish RestaurantApplicationSubmittedEvent to Kafka once event DTOs are defined
        return ApplicationResponse.from(applicationRepository.save(app));
    }

    @Override
    public ApplicationResponse reopenApplication(UUID appId, UUID ownerId) {
        RestaurantApplication app = loadOwnerApplication(appId, ownerId);

        if (app.getStatus() != ApplicationStatus.REJECTED) {
            throw new ApplicationStateException(
                    "Only REJECTED applications can be reopened. Current status: " + app.getStatus());
        }

        app.setStatus(ApplicationStatus.DRAFT);
        app.setRejectionRemarks(null);
        return ApplicationResponse.from(applicationRepository.save(app));
    }

    // -------------------------------------------------------------------------
    // Admin operations
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationSummaryResponse> listApplications(ApplicationStatus status, Pageable pageable) {
        return applicationRepository.findByStatus(status, pageable)
                .map(ApplicationSummaryResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationAsAdmin(UUID appId) {
        RestaurantApplication app = applicationRepository.findById(appId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + appId));
        return ApplicationResponse.from(app);
    }

    @Override
    public void approveApplication(UUID appId, UUID adminId) {
        RestaurantApplication app = applicationRepository.findById(appId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + appId));

        if (app.getStatus() != ApplicationStatus.SUBMITTED && app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new ApplicationStateException(
                    "Only SUBMITTED or UNDER_REVIEW applications can be approved. Current status: " + app.getStatus());
        }

        User admin = loadUser(adminId);
        Restaurant restaurant = promoteToRestaurant(app, admin);

        app.setRestaurant(restaurant);
        app.setStatus(ApplicationStatus.APPROVED);
        app.setReviewedBy(admin);
        app.setReviewedAt(Instant.now());
        applicationRepository.save(app);
        // TODO: publish RestaurantApprovedEvent to Kafka
    }

    @Override
    public void rejectApplication(UUID appId, UUID adminId, String remarks) {
        RestaurantApplication app = applicationRepository.findById(appId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + appId));

        if (app.getStatus() != ApplicationStatus.SUBMITTED && app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new ApplicationStateException(
                    "Only SUBMITTED or UNDER_REVIEW applications can be rejected. Current status: " + app.getStatus());
        }

        User admin = loadUser(adminId);
        app.setStatus(ApplicationStatus.REJECTED);
        app.setRejectionRemarks(remarks);
        app.setReviewedBy(admin);
        app.setReviewedAt(Instant.now());
        applicationRepository.save(app);
        // TODO: publish RestaurantRejectedEvent to Kafka
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
        restaurant = restaurantRepository.save(restaurant);

        final Restaurant savedRestaurant = restaurant;

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
        RestaurantVerificationStatusHistory history = new RestaurantVerificationStatusHistory();
        history.setRestaurant(savedRestaurant);
        history.setReviewedBy(admin);
        history.setStatus(RestaurantVerificationStatus.APPROVED);
        statusHistoryRepository.save(history);

        return savedRestaurant;
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
