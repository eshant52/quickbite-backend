package com.quickbite.quickbite.review.controller;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.review.dto.RestaurantRatingSummaryResponse;
import com.quickbite.quickbite.review.dto.ReviewResponse;
import com.quickbite.quickbite.review.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/reviews")
public class RestaurantReviewController {

    private final ReviewService reviewService;

    public RestaurantReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<CursorPage<ReviewResponse>> getRestaurantReviews(
            @PathVariable UUID restaurantId,
            @RequestParam(value = "cursor", required = false) UUID cursor,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(reviewService.getRestaurantReviews(restaurantId, cursor, size));
    }

    @GetMapping("/summary")
    public ResponseEntity<RestaurantRatingSummaryResponse> getRestaurantRatingSummary(
            @PathVariable UUID restaurantId
    ) {
        return ResponseEntity.ok(reviewService.getRestaurantRatingSummary(restaurantId));
    }
}
