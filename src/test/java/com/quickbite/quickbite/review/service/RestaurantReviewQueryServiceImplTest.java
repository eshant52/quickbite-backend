package com.quickbite.quickbite.review.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.restaurant.exception.RestaurantNotFoundException;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.quickbite.review.dto.RestaurantRatingSummaryResponse;
import com.quickbite.quickbite.review.dto.ReviewResponse;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantReviewQueryServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private RestaurantReviewQueryServiceImpl queryService;

    private Restaurant restaurant;
    private UUID restaurantId;

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();

        restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setName("Taco Haven");
        restaurant.setAvgRating(BigDecimal.valueOf(4.00));
        restaurant.setTotalRating(10L);
    }

    @Test
    @DisplayName("getRestaurantReviews - returns cursor page of reviews for restaurant")
    void getRestaurantReviews_Success() {
        Review review = new Review();
        review.setId(UUID.randomUUID());
        review.setCustomer(new User());
        review.setRestaurant(restaurant);
        review.setOrder(new Order());
        review.setRating(5);

        when(restaurantRepository.existsById(restaurantId)).thenReturn(true);
        when(reviewRepository.findByRestaurantWithCursor(eq(restaurantId), eq(null), any(Limit.class)))
                .thenReturn(List.of(review));

        CursorPage<ReviewResponse> page = queryService.getRestaurantReviews(restaurantId, null, 20);

        assertThat(page.content()).hasSize(1);
    }

    @Test
    @DisplayName("getRestaurantReviews - throws 404 when restaurant not found")
    void getRestaurantReviews_NotFound() {
        when(restaurantRepository.existsById(restaurantId)).thenReturn(false);

        assertThatThrownBy(() -> queryService.getRestaurantReviews(restaurantId, null, 20))
                .isInstanceOf(RestaurantNotFoundException.class);
    }

    @Test
    @DisplayName("getRestaurantRatingSummary - returns rating summary with distribution")
    void getRestaurantRatingSummary_Success() {
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        List<Object[]> distribution = List.<Object[]>of(
                new Object[]{5, 6L},
                new Object[]{4, 3L},
                new Object[]{1, 1L}
        );
        when(reviewRepository.getRatingDistributionForRestaurant(restaurantId)).thenReturn(distribution);

        RestaurantRatingSummaryResponse summary = queryService.getRestaurantRatingSummary(restaurantId);

        assertThat(summary.restaurantId()).isEqualTo(restaurantId);
        assertThat(summary.avgRating()).isEqualTo(BigDecimal.valueOf(4.00));
        assertThat(summary.totalRating()).isEqualTo(10L);
        assertThat(summary.ratingDistribution().get(5)).isEqualTo(6L);
        assertThat(summary.ratingDistribution().get(4)).isEqualTo(3L);
        assertThat(summary.ratingDistribution().get(3)).isEqualTo(0L);
        assertThat(summary.ratingDistribution().get(1)).isEqualTo(1L);
    }

    @Test
    @DisplayName("getRestaurantRatingSummary - throws 404 when restaurant not found")
    void getRestaurantRatingSummary_NotFound() {
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.getRestaurantRatingSummary(restaurantId))
                .isInstanceOf(RestaurantNotFoundException.class);
    }
}
