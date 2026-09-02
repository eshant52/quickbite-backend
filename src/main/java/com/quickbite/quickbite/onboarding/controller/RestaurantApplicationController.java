package com.quickbite.quickbite.onboarding.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.onboarding.dto.restaurant.*;
import com.quickbite.quickbite.onboarding.service.restaurant.RestaurantApplicationService;
import com.quickbite.quickbite.restaurant.model.RestaurantDocumentType;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Owner-facing REST controller for the multistep restaurant onboarding flow.
 * All endpoints require ROLE_RESTAURANT_OWNER + SCOPE_API (enforced by SecurityConfig
 * at URL level and @PreAuthorize at method level as inner guard).
 */
@RestController
@RequestMapping("/api/v1/restaurant/applications")
@PreAuthorize("hasRole('RESTAURANT_OWNER')")
public class RestaurantApplicationController {

    private final RestaurantApplicationService applicationService;
    private final AuthenticatedSessionResolver sessionResolver;

    public RestaurantApplicationController(RestaurantApplicationService applicationService,
                                           AuthenticatedSessionResolver sessionResolver) {
        this.applicationService = applicationService;
        this.sessionResolver = sessionResolver;
    }

    // ─── Step 0: Create / Get Application ────────────────────────────────────

    /**
     * Start a new draft application. Returns 409 if an active one already exists.
     */
    @PostMapping("/start")
    public ResponseEntity<RestaurantApplicationResponse> startApplication(@AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.startApplication(ownerId));
    }

    /**
     * Resume the current in-progress (DRAFT/SUBMITTED/UNDER_REVIEW) application.
     */
    @GetMapping("/current")
    public ResponseEntity<RestaurantApplicationResponse> getCurrentApplication(@AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.getCurrentApplication(ownerId));
    }

    /**
     * Get a specific application by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RestaurantApplicationResponse> getApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.getApplication(id, ownerId));
    }

    // ─── Step 1: Basic Details ───────────────────────────────────────────────

    /**
     * Step 1 — Get restaurant basic details (name, description).
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<RestaurantApplicationDetailsResponse> getDetails(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.getDetails(id, ownerId));
    }

    /**
     * Step 1 — Save restaurant name and description.
     */
    @PatchMapping("/{id}/details")
    public ResponseEntity<RestaurantApplicationDetailsResponse> saveDetails(
            @PathVariable UUID id,
            @Valid @RequestBody RestaurantApplicationDetailsRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.saveDetails(id, ownerId, request));
    }

    // ─── Step 2: Address ─────────────────────────────────────────────────────

    /**
     * Step 2 — Get restaurant address details.
     */
    @GetMapping("/{id}/address")
    public ResponseEntity<RestaurantApplicationAddressResponse> getAddress(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.getAddress(id, ownerId));
    }

    /**
     * Step 2 — Save restaurant address with lat/lon coordinates.
     */
    @PatchMapping("/{id}/address")
    public ResponseEntity<RestaurantApplicationAddressResponse> saveAddress(
            @PathVariable UUID id,
            @Valid @RequestBody RestaurantApplicationAddressRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.saveAddress(id, ownerId, request));
    }

    // ─── Step 3: Operating Hours ─────────────────────────────────────────────

    /**
     * Step 3 — Get operating hours for this application.
     */
    @GetMapping("/{id}/hours")
    public ResponseEntity<List<RestaurantApplicationHoursResponse>> getHours(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.getHours(id, ownerId));
    }

    /**
     * Step 3 — Save operating hours (replaces all existing hours for this application).
     */
    @PostMapping("/{id}/hours")
    public ResponseEntity<List<RestaurantApplicationHoursResponse>> saveHours(
            @PathVariable UUID id,
            @Valid @RequestBody RestaurantApplicationHoursRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.saveHours(id, ownerId, request));
    }

    // ─── Step 4: Images ──────────────────────────────────────────────────────

    /**
     * Step 4 — Get restaurant images for this application.
     */
    @GetMapping("/{id}/images")
    public ResponseEntity<List<RestaurantApplicationImageResponse>> getImages(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.getImage(id, ownerId));
    }

    /**
     * Step 4 — Add a restaurant image using its S3 URL (client uploads to S3 first).
     */
    @PostMapping("/{id}/images")
    public ResponseEntity<RestaurantApplicationImageResponse> addImage(
            @PathVariable UUID id,
            @Valid @RequestBody RestaurantApplicationImageRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.addImage(id, ownerId, request));
    }

    /**
     * Remove a previously added image.
     */
    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> removeImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        applicationService.removeImage(id, ownerId, imageId);
        return ResponseEntity.noContent().build();
    }

    // ─── Step 5: Documents ───────────────────────────────────────────────────

    /**
     * Step 5 — Get all documents uploaded for this application.
     */
    @GetMapping("/{id}/documents")
    public ResponseEntity<List<RestaurantApplicationDocumentResponse>> getDocuments(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.getDocuments(id, ownerId));
    }

    /**
     * Step 5 — Add or replace a document of a given type using its S3 URL.
     */
    @PostMapping("/{id}/documents")
    public ResponseEntity<RestaurantApplicationDocumentResponse> addDocument(
            @PathVariable UUID id,
            @Valid @RequestBody RestaurantApplicationDocumentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.addDocument(id, ownerId, request));
    }

    /**
     * Remove a previously uploaded document.
     */
    @DeleteMapping("/{id}/documents/{type}")
    public ResponseEntity<Void> removeDocument(
            @PathVariable UUID id,
            @PathVariable RestaurantDocumentType type,
            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        applicationService.removeDocument(id, ownerId, type);
        return ResponseEntity.noContent().build();
    }

    // ─── Step 6: Submit / Reopen ─────────────────────────────────────────────

    /**
     * Submit the completed application for admin review (DRAFT → SUBMITTED).
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<RestaurantApplicationResponse> submitApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.submitApplication(id, ownerId));
    }

    /**
     * Reopen a rejected application so the owner can edit and resubmit (REJECTED → DRAFT).
     */
    @PostMapping("/{id}/reopen")
    public ResponseEntity<RestaurantApplicationResponse> reopenApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.reopenApplication(id, ownerId));
    }
}
