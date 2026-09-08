package com.quickbite.quickbite.review.controller;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.review.dto.ReviewResponse;
import com.quickbite.quickbite.review.service.RestaurantReviewManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class RestaurantReviewManagementController {

    private final RestaurantReviewManagementService restaurantReviewManagementService;

    public RestaurantReviewManagementController(RestaurantReviewManagementService restaurantReviewManagementService) {
        this.restaurantReviewManagementService = restaurantReviewManagementService;
    }

    @GetMapping
    public ResponseEntity<CursorPage<ReviewResponse>> listReviews(
            @RequestParam(value = "restaurantId", required = false) UUID restaurantId,
            @RequestParam(value = "cursor", required = false) UUID cursor,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(restaurantReviewManagementService.listReviews(restaurantId, cursor, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReview(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(restaurantReviewManagementService.getReview(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable UUID id
    ) {
        restaurantReviewManagementService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
