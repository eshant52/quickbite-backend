package com.quickbite.quickbite.user.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.user.dto.UpdateProfileRequest;
import com.quickbite.quickbite.user.dto.UserProfileResponse;
import com.quickbite.quickbite.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/me")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerProfileController {

    private final UserProfileService userProfileService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public CustomerProfileController(
            UserProfileService userProfileService,
            AuthenticatedSessionResolver authenticatedSessionResolver) {
        this.userProfileService = userProfileService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(userProfileService.getProfile(userId));
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UpdateProfileRequest req) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(userProfileService.updateProfile(userId, req));
    }
}
