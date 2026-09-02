package com.quickbite.quickbite.review.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.review.dto.CreateReviewRequest;
import com.quickbite.quickbite.review.dto.ReviewResponse;
import com.quickbite.quickbite.review.dto.UpdateReviewRequest;
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
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @Mock
    private AuthenticatedSessionResolver authenticatedSessionResolver;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private CustomerReviewController controller;

    private UUID customerId;
    private UUID reviewId;
    private UUID restaurantId;
    private UUID orderId;
    private ReviewResponse mockResponse;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        reviewId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        mockResponse = new ReviewResponse(
                reviewId,
                restaurantId,
                "Burger Barn",
                customerId,
                "John Customer",
                orderId,
                5,
                "Delicious burgers!",
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("submitReview - returns HTTP 201 Created with ReviewResponse")
    void submitReview_Success() {
        CreateReviewRequest request = new CreateReviewRequest(orderId, 5, "Delicious burgers!");
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(customerId);
        when(reviewService.submitReview(customerId, request)).thenReturn(mockResponse);

        ResponseEntity<ReviewResponse> response = controller.submitReview(jwt, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(mockResponse);
    }

    @Test
    @DisplayName("getMyReviews - returns HTTP 200 OK with CursorPage")
    void getMyReviews_Success() {
        CursorPage<ReviewResponse> mockPage = new CursorPage<>(List.of(mockResponse), null, false, 1);
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(customerId);
        when(reviewService.getMyReviews(customerId, null, 20)).thenReturn(mockPage);

        ResponseEntity<CursorPage<ReviewResponse>> response = controller.getMyReviews(jwt, null, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockPage);
    }

    @Test
    @DisplayName("getReview - returns HTTP 200 OK with ReviewResponse")
    void getReview_Success() {
        when(reviewService.getReview(reviewId)).thenReturn(mockResponse);

        ResponseEntity<ReviewResponse> response = controller.getReview(reviewId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockResponse);
    }

    @Test
    @DisplayName("updateReview - returns HTTP 200 OK with updated ReviewResponse")
    void updateReview_Success() {
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated comment");
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(customerId);
        when(reviewService.updateReview(reviewId, customerId, request)).thenReturn(mockResponse);

        ResponseEntity<ReviewResponse> response = controller.updateReview(jwt, reviewId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockResponse);
    }

    @Test
    @DisplayName("deleteReview - returns HTTP 204 No Content")
    void deleteReview_Success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(customerId);

        ResponseEntity<Void> response = controller.deleteReview(jwt, reviewId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(reviewService).deleteReview(reviewId, customerId);
    }
}
