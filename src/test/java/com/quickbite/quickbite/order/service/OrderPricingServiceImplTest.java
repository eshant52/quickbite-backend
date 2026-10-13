package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.cart.model.Cart;
import com.quickbite.quickbite.common.config.property.DeliveryFeeProperties;
import com.quickbite.quickbite.common.config.property.PricingProperties;
import com.quickbite.quickbite.common.dto.PriceBreakdown;
import com.quickbite.quickbite.common.routing.GeoPoint;
import com.quickbite.quickbite.common.routing.RouteResult;
import com.quickbite.quickbite.common.routing.RoutingGateway;
import com.quickbite.quickbite.order.service.fee.BaseFeeCalculator;
import com.quickbite.quickbite.order.service.fee.DeliveryFeeCalculator;
import com.quickbite.quickbite.order.service.fee.DistanceFeeCalculator;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.user.model.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPricingServiceImplTest {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Mock private RoutingGateway routingGateway;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private OrderPricingServiceImpl pricingService;
    private DeliveryFeeProperties feeProperties;
    private PricingProperties pricingProperties;

    private Cart cart;
    private Address restaurantAddress;
    private Address customerAddress;

    @BeforeEach
    void setUp() {
        feeProperties = new DeliveryFeeProperties(
                BigDecimal.valueOf(15.00),
                BigDecimal.valueOf(8.00),
                BigDecimal.valueOf(20.00),
                BigDecimal.valueOf(80.00)
        );

        pricingProperties = new PricingProperties(
                BigDecimal.valueOf(5.00),
                BigDecimal.valueOf(0.05)
        );

        List<DeliveryFeeCalculator> calculators = List.of(
                new BaseFeeCalculator(feeProperties),
                new DistanceFeeCalculator(feeProperties)
        );

        pricingService = new OrderPricingServiceImpl(
                routingGateway,
                calculators,
                feeProperties,
                pricingProperties,
                stringRedisTemplate
        );

        restaurantAddress = new Address();
        restaurantAddress.setId(UUID.randomUUID());
        restaurantAddress.setLocation(GF.createPoint(new Coordinate(77.5946, 12.9716)));

        Restaurant restaurant = new Restaurant();
        restaurant.setId(UUID.randomUUID());
        restaurant.setName("Bella Italia");
        restaurant.setAddress(restaurantAddress);

        customerAddress = new Address();
        customerAddress.setId(UUID.randomUUID());
        customerAddress.setLocation(GF.createPoint(new Coordinate(77.6245, 12.9352)));

        cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setRestaurant(restaurant);
        cart.setTotalPrice(BigDecimal.valueOf(500.00));
    }

    @Test
    @DisplayName("Calculates complete price breakdown including subtotal, tax, fees, tip, and total")
    void calculatePrice_success() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(routingGateway.route(any(GeoPoint.class), any(GeoPoint.class)))
                .thenReturn(new RouteResult(3000.0, 600)); // 3km -> base 15 + (3 * 8 = 24) = 39.00 fee

        PriceBreakdown result = pricingService.calculatePrice(cart, customerAddress, BigDecimal.valueOf(25.00));

        assertThat(result).isNotNull();
        assertThat(result.subTotal()).isEqualByComparingTo(BigDecimal.valueOf(500.00));
        assertThat(result.discountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.deliveryFee()).isEqualByComparingTo(BigDecimal.valueOf(39.00));
        assertThat(result.platformFee()).isEqualByComparingTo(BigDecimal.valueOf(5.00));
        assertThat(result.taxAmount()).isEqualByComparingTo(BigDecimal.valueOf(25.00)); // 5% of 500
        assertThat(result.tipAmount()).isEqualByComparingTo(BigDecimal.valueOf(25.00));
        // total = 500 + 39 + 5 + 25 + 25 = 594.00
        assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(594.00));
        assertThat(result.deliveryDistanceMeters()).isEqualTo(3000.0);
        assertThat(result.estimatedDeliverySeconds()).isEqualTo(600L);

        // Verify route cached in Redis
        verify(valueOperations).set(contains("quickbite:route:"), eq("3000.0:600"), any(Duration.class));
    }

    @Test
    @DisplayName("Uses cached route from Redis when available without calling routing provider")
    void calculatePrice_usesRedisCachedRoute() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("3000.0:600");

        PriceBreakdown result = pricingService.calculatePrice(cart, customerAddress, BigDecimal.ZERO);

        assertThat(result.deliveryDistanceMeters()).isEqualTo(3000.0);
        assertThat(result.estimatedDeliverySeconds()).isEqualTo(600L);
        assertThat(result.deliveryFee()).isEqualByComparingTo(BigDecimal.valueOf(39.00));

        // Verify routing gateway was NOT invoked
        verify(routingGateway, never()).route(any(), any());
    }

    @Test
    @DisplayName("Applies minimum delivery fee clamp when computed fee is below minFee")
    void calculatePrice_clampsToMinFee() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(routingGateway.route(any(GeoPoint.class), any(GeoPoint.class)))
                .thenReturn(new RouteResult(200.0, 60)); // 0.2km -> base 15 + 1.60 = 16.60 -> clamped to min 20.00

        PriceBreakdown result = pricingService.calculatePrice(cart, customerAddress, null);

        assertThat(result.deliveryFee()).isEqualByComparingTo(BigDecimal.valueOf(20.00));
        assertThat(result.tipAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Applies maximum delivery fee clamp when computed fee exceeds maxFee")
    void calculatePrice_clampsToMaxFee() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(routingGateway.route(any(GeoPoint.class), any(GeoPoint.class)))
                .thenReturn(new RouteResult(20000.0, 3600)); // 20km -> base 15 + 160 = 175 -> clamped to max 80.00

        PriceBreakdown result = pricingService.calculatePrice(cart, customerAddress, null);

        assertThat(result.deliveryFee()).isEqualByComparingTo(BigDecimal.valueOf(80.00));
    }

    @Test
    @DisplayName("Returns 0 distance and fallback route when address locations are null")
    void calculatePrice_nullLocationFallback() {
        customerAddress.setLocation(null);

        PriceBreakdown result = pricingService.calculatePrice(cart, customerAddress, null);

        assertThat(result.deliveryDistanceMeters()).isEqualTo(0.0);
        assertThat(result.estimatedDeliverySeconds()).isEqualTo(0L);
        assertThat(result.deliveryFee()).isEqualByComparingTo(BigDecimal.valueOf(20.00)); // Clamped to minFee
        verify(routingGateway, never()).route(any(), any());
    }
}
