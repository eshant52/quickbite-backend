package com.quickbite.quickbite.menu.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.menu.dto.CuisineRequest;
import com.quickbite.quickbite.menu.dto.CuisineRequestResponse;
import com.quickbite.quickbite.menu.service.CuisineOwnerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cuisines")
@PreAuthorize("hasRole('RESTAURANT_OWNER')")
public class CuisineOwnerController {

    private final CuisineOwnerService cuisineOwnerService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public CuisineOwnerController(
            CuisineOwnerService cuisineOwnerService,
            AuthenticatedSessionResolver authenticatedSessionResolver) {
        this.cuisineOwnerService = cuisineOwnerService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    @PostMapping
    public ResponseEntity<CuisineRequestResponse> requestCuisine(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CuisineRequest cuisineRequest) {
        UUID requesterId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cuisineOwnerService.request(cuisineRequest, requesterId));
    }
}
