package com.quickbite.quickbite.menu.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.menu.dto.CuisineRequest;
import com.quickbite.quickbite.menu.dto.CuisineRequestResponse;
import com.quickbite.quickbite.menu.dto.CuisineResponse;
import com.quickbite.quickbite.menu.service.CuisineService;
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
@RequestMapping("/api/v1/cuisines")
public class CuisineController {

    private final CuisineService cuisineService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public CuisineController(
            CuisineService cuisineService,
            AuthenticatedSessionResolver authenticatedSessionResolver) {
        this.cuisineService = cuisineService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    @GetMapping
    public ResponseEntity<List<CuisineResponse>> getApprovedCuisines() {
        return ResponseEntity.ok(cuisineService.listApproved());
    }

    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<CuisineRequestResponse> requestCuisine(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CuisineRequest cuisineRequest) {
        UUID requesterId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cuisineService.request(cuisineRequest, requesterId));
    }
}
