package com.quickbite.quickbite.menu.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.menu.dto.AdminRejectRequest;
import com.quickbite.quickbite.menu.dto.CuisineResponse;
import com.quickbite.quickbite.menu.model.CuisineStatus;
import com.quickbite.quickbite.menu.service.CuisineService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/cuisines")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCuisineController {

    private final CuisineService cuisineService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public AdminCuisineController(
            CuisineService cuisineService,
            AuthenticatedSessionResolver authenticatedSessionResolver
    ) {
        this.cuisineService = cuisineService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    @GetMapping
    public ResponseEntity<CursorPage<CuisineResponse>> getAdminCuisines(
            @RequestParam(defaultValue = "PENDING") CuisineStatus status,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(cuisineService.listByStatus(status, cursor, size));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<CuisineResponse> approveCuisine(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID adminId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(cuisineService.approve(id, adminId));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<CuisineResponse> rejectCuisine(
            @RequestBody @Valid AdminRejectRequest rejectRequest,
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID adminId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(cuisineService.reject(id, adminId, rejectRequest.remarks()));
    }
}
