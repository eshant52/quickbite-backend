package com.quickbite.quickbite.review.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.restaurant.exception.RestaurantNotFoundException;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.quickbite.review.dto.CreateReviewRequest;
import com.quickbite.quickbite.review.dto.RestaurantRatingSummaryResponse;
import com.quickbite.quickbite.review.dto.ReviewResponse;
import com.quickbite.quickbite.review.dto.UpdateReviewRequest;
import com.quickbite.quickbite.review.exception.DuplicateReviewException;
import com.quickbite.quickbite.review.exception.InvalidReviewException;
import com.quickbite.quickbite.review.exception.ReviewNotFoundException;
import com.quickbite.quickbite.review.model.Review;
import com.quickbite.quickbite.review.repository.ReviewRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User customer;
    private Restaurant restaurant;
    private Order order;
    private UUID customerId;
    private UUID restaurantId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        customer = new User();
        customer.setId(customerId);
        customer.setName("John Customer");

        restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setName("Taco Haven");
        restaurant.setAvgRating(BigDecimal.valueOf(4.00));
        restaurant.setTotalRating(10L);

        order = new Order();
        order.setId(orderId);
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setCurrentStatus(OrderStatus.DELIVERED);
    }

    @Nested
    @DisplayName("submitReview")
    class SubmitReviewTests {

        @Test
        @DisplayName("Successfully submits a review for delivered order and updates restaurant aggregate")
        void submitReview_Success() {
            CreateReviewRequest request = new CreateReviewRequest(orderId, 5, "Amazing tacos!");

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(reviewRepository.existsByOrderId(orderId)).thenReturn(false);
            when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
            when(reviewRepository.getAverageRatingForRestaurant(restaurantId)).thenReturn(4.50);
            when(reviewRepository.countByRestaurantId(restaurantId)).thenReturn(11L);

            when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
                Review r = invocation.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });

            ReviewResponse response = reviewService.submitReview(customerId, request);

            assertThat(response).isNotNull();
            assertThat(response.rating()).isEqualTo(5);
            assertThat(response.comment()).isEqualTo("Amazing tacos!");
            assertThat(response.restaurantName()).isEqualTo("Taco Haven");

            verify(restaurantRepository).save(restaurant);
            assertThat(restaurant.getAvgRating()).isEqualTo(BigDecimal.valueOf(4.50).setScale(2));
            assertThat(restaurant.getTotalRating()).isEqualTo(11L);
        }

        @Test
        @DisplayName("Fails if order does not exist")
        void submitReview_OrderNotFound() {
            CreateReviewRequest request = new CreateReviewRequest(orderId, 5, "Good");
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.submitReview(customerId, request))
                    .isInstanceOf(InvalidReviewException.class)
                    .hasMessageContaining("Order not found");
        }

        @Test
        @DisplayName("Fails if order was placed by different customer")
        void submitReview_DifferentCustomer() {
            UUID otherCustomerId = UUID.randomUUID();
            CreateReviewRequest request = new CreateReviewRequest(orderId, 5, "Good");
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> reviewService.submitReview(otherCustomerId, request))
                    .isInstanceOf(InvalidReviewException.class)
                    .hasMessageContaining("own account");
        }

        @Test
        @DisplayName("Fails if order is not in DELIVERED state")
        void submitReview_OrderNotDelivered() {
            order.setCurrentStatus(OrderStatus.OUT_FOR_DELIVERY);
            CreateReviewRequest request = new CreateReviewRequest(orderId, 5, "Good");
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> reviewService.submitReview(customerId, request))
                    .isInstanceOf(InvalidReviewException.class)
                    .hasMessageContaining("DELIVERED");
        }

        @Test
        @DisplayName("Fails if review already submitted for this order")
        void submitReview_Duplicate() {
            CreateReviewRequest request = new CreateReviewRequest(orderId, 5, "Good");
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(reviewRepository.existsByOrderId(orderId)).thenReturn(true);

            assertThatThrownBy(() -> reviewService.submitReview(customerId, request))
                    .isInstanceOf(DuplicateReviewException.class);
        }
    }

    @Nested
    @DisplayName("getReview & getMyReviews")
    class GetReviewTests {

        @Test
        @DisplayName("Gets single review by ID")
        void getReview_Success() {
            UUID reviewId = UUID.randomUUID();
            Review review = new Review();
            review.setId(reviewId);
            review.setCustomer(customer);
            review.setRestaurant(restaurant);
            review.setOrder(order);
            review.setRating(4);

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

            ReviewResponse response = reviewService.getReview(reviewId);

            assertThat(response.id()).isEqualTo(reviewId);
            assertThat(response.rating()).isEqualTo(4);
        }

        @Test
        @DisplayName("Throws 404 when review not found")
        void getReview_NotFound() {
            UUID reviewId = UUID.randomUUID();
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.getReview(reviewId))
                    .isInstanceOf(ReviewNotFoundException.class);
        }

        @Test
        @DisplayName("Gets paginated customer reviews")
        void getMyReviews_Success() {
            Review review = new Review();
            review.setId(UUID.randomUUID());
            review.setCustomer(customer);
            review.setRestaurant(restaurant);
            review.setOrder(order);
            review.setRating(5);

            when(reviewRepository.findByCustomerWithCursor(eq(customerId), eq(null), any(Limit.class)))
                    .thenReturn(List.of(review));

            CursorPage<ReviewResponse> page = reviewService.getMyReviews(customerId, null, 20);

            assertThat(page.content()).hasSize(1);
            assertThat(page.hasMore()).isFalse();
        }
    }

    @Nested
    @DisplayName("getRestaurantReviews & rating summary")
    class RestaurantReviewTests {

        @Test
        @DisplayName("Gets restaurant rating summary with star distribution")
        void getRestaurantRatingSummary_Success() {
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
            List<Object[]> distribution = List.<Object[]>of(
                    new Object[]{5, 6L},
                    new Object[]{4, 3L},
                    new Object[]{1, 1L}
            );
            when(reviewRepository.getRatingDistributionForRestaurant(restaurantId)).thenReturn(distribution);

            RestaurantRatingSummaryResponse summary = reviewService.getRestaurantRatingSummary(restaurantId);

            assertThat(summary.restaurantId()).isEqualTo(restaurantId);
            assertThat(summary.avgRating()).isEqualTo(BigDecimal.valueOf(4.00));
            assertThat(summary.totalRating()).isEqualTo(10L);
            assertThat(summary.ratingDistribution().get(5)).isEqualTo(6L);
            assertThat(summary.ratingDistribution().get(4)).isEqualTo(3L);
            assertThat(summary.ratingDistribution().get(3)).isEqualTo(0L);
            assertThat(summary.ratingDistribution().get(1)).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("updateReview & deleteReview")
    class UpdateAndDeleteTests {

        @Test
        @DisplayName("Updates review and recalculates restaurant average")
        void updateReview_Success() {
            UUID reviewId = UUID.randomUUID();
            Review review = new Review();
            review.setId(reviewId);
            review.setCustomer(customer);
            review.setRestaurant(restaurant);
            review.setOrder(order);
            review.setRating(3);
            review.setComment("Okay");

            UpdateReviewRequest request = new UpdateReviewRequest(5, "Much better on second try!");

            when(reviewRepository.findByIdAndCustomerId(reviewId, customerId)).thenReturn(Optional.of(review));
            when(reviewRepository.save(any(Review.class))).thenReturn(review);
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
            when(reviewRepository.getAverageRatingForRestaurant(restaurantId)).thenReturn(4.80);
            when(reviewRepository.countByRestaurantId(restaurantId)).thenReturn(10L);

            ReviewResponse response = reviewService.updateReview(reviewId, customerId, request);

            assertThat(response.rating()).isEqualTo(5);
            assertThat(response.comment()).isEqualTo("Much better on second try!");
            verify(restaurantRepository).save(restaurant);
        }

        @Test
        @DisplayName("Deletes review and recalculates restaurant average")
        void deleteReview_Success() {
            UUID reviewId = UUID.randomUUID();
            Review review = new Review();
            review.setId(reviewId);
            review.setCustomer(customer);
            review.setRestaurant(restaurant);
            review.setOrder(order);

            when(reviewRepository.findByIdAndCustomerId(reviewId, customerId)).thenReturn(Optional.of(review));
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
            when(reviewRepository.getAverageRatingForRestaurant(restaurantId)).thenReturn(4.10);
            when(reviewRepository.countByRestaurantId(restaurantId)).thenReturn(9L);

            reviewService.deleteReview(reviewId, customerId);

            verify(reviewRepository).delete(review);
            verify(restaurantRepository).save(restaurant);
        }
    }
}
