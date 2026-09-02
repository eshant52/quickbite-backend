package com.quickbite.quickbite.review.controller;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.review.dto.RestaurantRatingSummaryResponse;
import com.quickbite.quickbite.review.dto.ReviewResponse;
import com.quickbite.quickbite.review.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private RestaurantReviewController controller;

    private UUID restaurantId;
    private ReviewResponse mockReview;

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();

        mockReview = new ReviewResponse(
                UUID.randomUUID(),
                restaurantId,
                "Burger Barn",
                UUID.randomUUID(),
                "Alice",
                UUID.randomUUID(),
                5,
                "Great food!",
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("getRestaurantReviews - returns HTTP 200 OK with CursorPage")
    void getRestaurantReviews_Success() {
        CursorPage<ReviewResponse> mockPage = new CursorPage<>(List.of(mockReview), null, false, 1);
        when(reviewService.getRestaurantReviews(restaurantId, null, 20)).thenReturn(mockPage);

        ResponseEntity<CursorPage<ReviewResponse>> response = controller.getRestaurantReviews(restaurantId, null, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockPage);
    }

    @Test
    @DisplayName("getRestaurantRatingSummary - returns HTTP 200 OK with summary and distribution")
    void getRestaurantRatingSummary_Success() {
        RestaurantRatingSummaryResponse summary = new RestaurantRatingSummaryResponse(
                restaurantId,
                BigDecimal.valueOf(4.50),
                10L,
                Map.of(5, 7L, 4, 2L, 3, 1L, 2, 0L, 1, 0L)
        );
        when(reviewService.getRestaurantRatingSummary(restaurantId)).thenReturn(summary);

        ResponseEntity<RestaurantRatingSummaryResponse> response = controller.getRestaurantRatingSummary(restaurantId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(summary);
    }
}
