package com.quickbite.quickbite.review.controller;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.review.dto.ReviewResponse;
import com.quickbite.quickbite.review.service.AdminReviewManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewManagementController {

    private final AdminReviewManagementService adminReviewManagementService;

    public AdminReviewManagementController(AdminReviewManagementService adminReviewManagementService) {
        this.adminReviewManagementService = adminReviewManagementService;
    }

    @GetMapping
    public ResponseEntity<CursorPage<ReviewResponse>> listReviews(
            @RequestParam(value = "restaurantId", required = false) UUID restaurantId,
            @RequestParam(value = "cursor", required = false) UUID cursor,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminReviewManagementService.listReviews(restaurantId, cursor, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReview(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(adminReviewManagementService.getReview(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable UUID id
    ) {
        adminReviewManagementService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
