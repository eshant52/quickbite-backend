package com.quickbite.quickbite.user.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.user.dto.AddressResponse;
import com.quickbite.quickbite.user.dto.CreateAddressRequest;
import com.quickbite.quickbite.user.service.UserAddressService;
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
@RequestMapping("/api/v1/customer/me/addresses")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerAddressController {

    private final UserAddressService userAddressService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public CustomerAddressController(
            UserAddressService userAddressService,
            AuthenticatedSessionResolver authenticatedSessionResolver) {
        this.userAddressService = userAddressService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAddresses(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(userAddressService.getAddresses(userId));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> addAddress(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CreateAddressRequest req) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(userAddressService.addAddress(userId, req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody @Valid CreateAddressRequest req) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(userAddressService.updateAddress(userId, id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        userAddressService.deleteAddress(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<AddressResponse> setDefaultAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(userAddressService.setDefaultAddress(userId, id));
    }
}
