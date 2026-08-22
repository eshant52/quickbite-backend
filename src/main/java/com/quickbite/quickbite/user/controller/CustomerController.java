package com.quickbite.quickbite.user.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.user.dto.AddressResponse;
import com.quickbite.quickbite.user.dto.CreateAddressRequest;
import com.quickbite.quickbite.user.dto.UpdateProfileRequest;
import com.quickbite.quickbite.user.dto.UserProfileResponse;
import com.quickbite.quickbite.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/me")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {
    private final UserService userService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public CustomerController(
    UserService userService,
    AuthenticatedSessionResolver authenticatedSessionResolver) {
        this.userService = userService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    // ---------------------------------------
    // Profile
    // ---------------------------------------

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal Jwt jwt) {
       return ResponseEntity.ok(userService.getProfile(authenticatedSessionResolver.userIdFromJwt(jwt)));
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UpdateProfileRequest req) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(userService.updateProfile(userId, req));
    }

    // ---------------------------------------
    // Address
    // ---------------------------------------

    @GetMapping("/addresses")
    public ResponseEntity<List<AddressResponse>> getAddresses(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(userService.getAddresses(userId));
    }

    @PostMapping("/addresses")
    public ResponseEntity<AddressResponse> addAddress(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CreateAddressRequest req) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addAddress(userId, req));
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody @Valid CreateAddressRequest req) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(userService.updateAddress(userId, id, req));
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        userService.deleteAddress(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/addresses/{id}/default")
    public ResponseEntity<AddressResponse> setDefaultAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(userService.setDefaultAddress(userId, id));
    }
}
