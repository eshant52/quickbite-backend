package com.quickbite.quickbite.review.controller;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.review.dto.ReviewResponse;
import com.quickbite.quickbite.review.service.AdminReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    public AdminReviewController(AdminReviewService adminReviewService) {
        this.adminReviewService = adminReviewService;
    }

    @GetMapping
    public ResponseEntity<CursorPage<ReviewResponse>> listAllReviews(
            @RequestParam(value = "restaurantId", required = false) UUID restaurantId,
            @RequestParam(value = "cursor", required = false) UUID cursor,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminReviewService.listAllReviews(restaurantId, cursor, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReviewAsAdmin(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(adminReviewService.getReviewAsAdmin(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReviewAsAdmin(
            @PathVariable UUID id
    ) {
        adminReviewService.deleteReviewAsAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
