package com.quickbite.quickbite.review.controller;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.review.dto.ReviewResponse;
import com.quickbite.quickbite.review.service.AdminReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReviewControllerTest {

    @Mock
    private AdminReviewService adminReviewService;

    @InjectMocks
    private AdminReviewController controller;

    private UUID reviewId;
    private UUID restaurantId;
    private ReviewResponse mockReview;

    @BeforeEach
    void setUp() {
        reviewId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();

        mockReview = new ReviewResponse(
                reviewId,
                restaurantId,
                "Burger Barn",
                UUID.randomUUID(),
                "Alice",
                UUID.randomUUID(),
                5,
                "Awesome!",
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("listAllReviews - returns HTTP 200 OK with CursorPage")
    void listAllReviews_Success() {
        CursorPage<ReviewResponse> mockPage = new CursorPage<>(List.of(mockReview), null, false, 1);
        when(adminReviewService.listAllReviews(restaurantId, null, 20)).thenReturn(mockPage);

        ResponseEntity<CursorPage<ReviewResponse>> response = controller.listAllReviews(restaurantId, null, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockPage);
    }

    @Test
    @DisplayName("getReviewAsAdmin - returns HTTP 200 OK with ReviewResponse")
    void getReviewAsAdmin_Success() {
        when(adminReviewService.getReviewAsAdmin(reviewId)).thenReturn(mockReview);

        ResponseEntity<ReviewResponse> response = controller.getReviewAsAdmin(reviewId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockReview);
    }

    @Test
    @DisplayName("deleteReviewAsAdmin - returns HTTP 204 No Content")
    void deleteReviewAsAdmin_Success() {
        ResponseEntity<Void> response = controller.deleteReviewAsAdmin(reviewId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(adminReviewService).deleteReviewAsAdmin(reviewId);
    }
}
