package com.quickbite.quickbite.review.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.review.dto.CreateReviewRequest;
import com.quickbite.quickbite.review.dto.ReviewResponse;
import com.quickbite.quickbite.review.dto.UpdateReviewRequest;
import com.quickbite.quickbite.review.service.CustomerReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/reviews")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerReviewController {

    private final CustomerReviewService customerReviewService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public CustomerReviewController(
            CustomerReviewService customerReviewService,
            AuthenticatedSessionResolver authenticatedSessionResolver
    ) {
        this.customerReviewService = customerReviewService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> submitReview(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CreateReviewRequest request
    ) {
        UUID customerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerReviewService.submitReview(customerId, request));
    }

    @GetMapping
    public ResponseEntity<CursorPage<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "cursor", required = false) UUID cursor,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        UUID customerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(customerReviewService.getMyReviews(customerId, cursor, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID customerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(customerReviewService.getReview(id, customerId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody @Valid UpdateReviewRequest request
    ) {
        UUID customerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(customerReviewService.updateReview(id, customerId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        UUID customerId = authenticatedSessionResolver.userIdFromJwt(jwt);
        customerReviewService.deleteReview(id, customerId);
        return ResponseEntity.noContent().build();
    }
}
