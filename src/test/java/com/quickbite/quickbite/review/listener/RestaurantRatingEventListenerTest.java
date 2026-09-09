package com.quickbite.quickbite.review.listener;

import com.quickbite.quickbite.common.event.review.ReviewChangedEvent;
import com.quickbite.quickbite.restaurant.exception.RestaurantNotFoundException;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.quickbite.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantRatingEventListenerTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private RestaurantRatingEventListener listener;

    private UUID restaurantId;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();
        restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setName("Pasta Palace");
        restaurant.setAvgRating(BigDecimal.ZERO);
        restaurant.setTotalRating(0L);
    }

    @Test
    @DisplayName("onReviewChanged - recalculates avgRating and totalRating when reviews exist")
    void onReviewChanged_Success_WithRatings() {
        when(restaurantRepository.findByIdForUpdate(restaurantId)).thenReturn(Optional.of(restaurant));
        when(reviewRepository.getAverageRatingForRestaurant(restaurantId)).thenReturn(4.6666);
        when(reviewRepository.countByRestaurantId(restaurantId)).thenReturn(15L);

        listener.onReviewChanged(new ReviewChangedEvent(restaurantId));

        verify(restaurantRepository).save(restaurant);
        assertThat(restaurant.getAvgRating()).isEqualTo(new BigDecimal("4.67"));
        assertThat(restaurant.getTotalRating()).isEqualTo(15L);
    }

    @Test
    @DisplayName("onReviewChanged - sets avgRating to ZERO when no reviews exist")
    void onReviewChanged_Success_WhenNoReviewsRemain() {
        when(restaurantRepository.findByIdForUpdate(restaurantId)).thenReturn(Optional.of(restaurant));
        when(reviewRepository.getAverageRatingForRestaurant(restaurantId)).thenReturn(null);
        when(reviewRepository.countByRestaurantId(restaurantId)).thenReturn(0L);

        listener.onReviewChanged(new ReviewChangedEvent(restaurantId));

        verify(restaurantRepository).save(restaurant);
        assertThat(restaurant.getAvgRating()).isEqualTo(BigDecimal.ZERO);
        assertThat(restaurant.getTotalRating()).isEqualTo(0L);
    }

    @Test
    @DisplayName("onReviewChanged - throws RestaurantNotFoundException when restaurant does not exist")
    void onReviewChanged_ThrowsException_WhenRestaurantNotFound() {
        when(restaurantRepository.findByIdForUpdate(restaurantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listener.onReviewChanged(new ReviewChangedEvent(restaurantId)))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessageContaining("Restaurant not found with id: " + restaurantId);
    }
}
