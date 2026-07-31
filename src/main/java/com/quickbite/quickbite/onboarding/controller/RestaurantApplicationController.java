package com.quickbite.quickbite.onboarding.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.onboarding.dto.*;
import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.service.RestaurantApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Owner-facing REST controller for the multi-step restaurant onboarding flow.
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

    /** Start a new draft application. Returns 409 if an active one already exists. */
    @PostMapping
    public ResponseEntity<ApplicationResponse> startApplication(@AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        ApplicationResponse response = applicationService.startApplication(ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Resume the current in-progress (DRAFT/SUBMITTED/UNDER_REVIEW) application. */
    @GetMapping("/current")
    public ResponseEntity<ApplicationResponse> getCurrentApplication(@AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.getCurrentApplication(ownerId));
    }

    /** Get a specific application by ID. */
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplication(@PathVariable UUID id,
                                                               @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.getApplication(id, ownerId));
    }

    /** Step 1 — Save restaurant name and description. */
    @PutMapping("/{id}/details")
    public ResponseEntity<ApplicationResponse> saveDetails(@PathVariable UUID id,
                                                            @Valid @RequestBody ApplicationDetailsRequest request,
                                                            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.saveDetails(id, ownerId, request));
    }

    /** Step 2 — Save restaurant address with lat/lon coordinates. */
    @PutMapping("/{id}/address")
    public ResponseEntity<ApplicationResponse> saveAddress(@PathVariable UUID id,
                                                            @Valid @RequestBody ApplicationAddressRequest request,
                                                            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.saveAddress(id, ownerId, request));
    }

    /** Step 3 — Save operating hours (replaces all existing hours for this application). */
    @PutMapping("/{id}/hours")
    public ResponseEntity<ApplicationResponse> saveHours(@PathVariable UUID id,
                                                          @Valid @RequestBody ApplicationHoursRequest request,
                                                          @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.saveHours(id, ownerId, request));
    }

    /** Step 4 — Add a restaurant image using its S3 URL (client uploads to S3 first). */
    @PostMapping("/{id}/images")
    public ResponseEntity<ApplicationResponse> addImage(@PathVariable UUID id,
                                                         @Valid @RequestBody ApplicationImageRequest request,
                                                         @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.addImage(id, ownerId, request));
    }

    /** Remove a previously added image. */
    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> removeImage(@PathVariable UUID id,
                                             @PathVariable UUID imageId,
                                             @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        applicationService.removeImage(id, ownerId, imageId);
        return ResponseEntity.noContent().build();
    }

    /** Step 5 — Add or replace a document of a given type using its S3 URL. */
    @PostMapping("/{id}/documents")
    public ResponseEntity<ApplicationResponse> addDocument(@PathVariable UUID id,
                                                            @Valid @RequestBody ApplicationDocumentRequest request,
                                                            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.addDocument(id, ownerId, request));
    }

    /** Remove a previously uploaded document. */
    @DeleteMapping("/{id}/documents/{documentId}")
    public ResponseEntity<Void> removeDocument(@PathVariable UUID id,
                                                @PathVariable UUID documentId,
                                                @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        applicationService.removeDocument(id, ownerId, documentId);
        return ResponseEntity.noContent().build();
    }

    /** Submit the completed application for admin review (DRAFT → SUBMITTED). */
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApplicationResponse> submitApplication(@PathVariable UUID id,
                                                                  @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.submitApplication(id, ownerId));
    }

    /** Reopen a rejected application so the owner can edit and resubmit (REJECTED → DRAFT). */
    @PostMapping("/{id}/reopen")
    public ResponseEntity<ApplicationResponse> reopenApplication(@PathVariable UUID id,
                                                                   @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(applicationService.reopenApplication(id, ownerId));
    }
}
