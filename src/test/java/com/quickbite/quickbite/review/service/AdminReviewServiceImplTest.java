package com.quickbite.quickbite.review.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.quickbite.review.dto.ReviewResponse;
import com.quickbite.quickbite.review.exception.ReviewNotFoundException;
import com.quickbite.quickbite.review.model.Review;
import com.quickbite.quickbite.review.repository.ReviewRepository;
import com.quickbite.quickbite.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private AdminReviewServiceImpl adminReviewService;

    private User customer;
    private Restaurant restaurant;
    private Order order;
    private Review review;
    private UUID reviewId;
    private UUID restaurantId;

    @BeforeEach
    void setUp() {
        reviewId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();

        customer = new User();
        customer.setId(UUID.randomUUID());
        customer.setName("Customer Jane");

        restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setName("Pasta Palace");

        order = new Order();
        order.setId(UUID.randomUUID());

        review = new Review();
        review.setId(reviewId);
        review.setCustomer(customer);
        review.setRestaurant(restaurant);
        review.setOrder(order);
        review.setRating(5);
        review.setComment("Delicious pasta!");
    }

    @Test
    @DisplayName("listAllReviews - returns cursor page of reviews")
    void listAllReviews_Success() {
        when(reviewRepository.findAllWithCursor(eq(restaurantId), eq(null), any(Limit.class)))
                .thenReturn(List.of(review));

        CursorPage<ReviewResponse> page = adminReviewService.listAllReviews(restaurantId, null, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().restaurantName()).isEqualTo("Pasta Palace");
    }

    @Test
    @DisplayName("getReviewAsAdmin - returns review details")
    void getReviewAsAdmin_Success() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        ReviewResponse response = adminReviewService.getReviewAsAdmin(reviewId);

        assertThat(response.id()).isEqualTo(reviewId);
        assertThat(response.comment()).isEqualTo("Delicious pasta!");
    }

    @Test
    @DisplayName("getReviewAsAdmin - throws 404 when not found")
    void getReviewAsAdmin_NotFound() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminReviewService.getReviewAsAdmin(reviewId))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    @DisplayName("deleteReviewAsAdmin - deletes review and updates restaurant rating aggregate")
    void deleteReviewAsAdmin_Success() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(reviewRepository.getAverageRatingForRestaurant(restaurantId)).thenReturn(4.20);
        when(reviewRepository.countByRestaurantId(restaurantId)).thenReturn(8L);

        adminReviewService.deleteReviewAsAdmin(reviewId);

        verify(reviewRepository).delete(review);
        verify(restaurantRepository).save(restaurant);
        assertThat(restaurant.getAvgRating()).isEqualTo(BigDecimal.valueOf(4.20).setScale(2));
        assertThat(restaurant.getTotalRating()).isEqualTo(8L);
    }
}
