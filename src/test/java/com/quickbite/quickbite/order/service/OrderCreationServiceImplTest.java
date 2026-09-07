package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.cart.model.Cart;
import com.quickbite.quickbite.cart.model.CartItem;
import com.quickbite.quickbite.cart.repository.CartRepository;
import com.quickbite.quickbite.common.routing.GeoPoint;
import com.quickbite.quickbite.common.routing.RouteResult;
import com.quickbite.quickbite.common.routing.RoutingGateway;
import com.quickbite.quickbite.menu.model.MenuItem;
import com.quickbite.quickbite.order.dto.PlaceOrderRequest;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.repository.OrderItemRepository;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.order.repository.OrderStatusHistoryRepository;
import com.quickbite.quickbite.order.service.fee.BaseFeeCalculator;
import com.quickbite.quickbite.order.service.fee.DeliveryFeeCalculator;
import com.quickbite.quickbite.common.config.property.DeliveryFeeProperties;
import com.quickbite.quickbite.order.service.fee.DistanceFeeCalculator;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.AddressRepository;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreationServiceImplTest {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private CartRepository cartRepository;
    @Mock private RoutingGateway routingGateway;

    private OrderCreationServiceImpl orderCreationService;
    private DeliveryFeeProperties feeProperties;

    private User customer;
    private Address address;
    private Restaurant restaurant;
    private Cart cart;
    private MenuItem menuItem;

    @BeforeEach
    void setUp() {
        feeProperties = new DeliveryFeeProperties(
                BigDecimal.valueOf(15.00),
                BigDecimal.valueOf(8.00),
                BigDecimal.valueOf(20.00),
                BigDecimal.valueOf(80.00)
        );

        List<DeliveryFeeCalculator> calculators = List.of(
                new BaseFeeCalculator(feeProperties),
                new DistanceFeeCalculator(feeProperties)
        );

        orderCreationService = new OrderCreationServiceImpl(
                orderRepository,
                orderItemRepository,
                orderStatusHistoryRepository,
                userRepository,
                addressRepository,
                cartRepository,
                routingGateway,
                calculators,
                feeProperties
        );

        UUID customerId = UUID.randomUUID();
        customer = new User();
        customer.setId(customerId);

        address = new Address();
        address.setId(UUID.randomUUID());
        address.setUser(customer);
        address.setStreet("123 Main St");
        address.setCity("Metropolis");
        address.setState("NY");
        address.setLocation(GF.createPoint(new Coordinate(77.6245, 12.9352))); // lng, lat

        Address resAddress = new Address();
        resAddress.setLocation(GF.createPoint(new Coordinate(77.5946, 12.9716)));

        restaurant = new Restaurant();
        restaurant.setId(UUID.randomUUID());
        restaurant.setName("Pasta Central");
        restaurant.setAddress(resAddress);

        menuItem = new MenuItem();
        menuItem.setId(UUID.randomUUID());
        menuItem.setName("Spaghetti Carbonara");
        menuItem.setPrice(BigDecimal.valueOf(200.00));
        menuItem.setAvailable(true);

        CartItem cartItem = new CartItem();
        cartItem.setMenuItem(menuItem);
        cartItem.setQuantity(2);
        cartItem.setUnitPrice(BigDecimal.valueOf(200.00));
        cartItem.setSubTotal(BigDecimal.valueOf(400.00));

        cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setCustomer(customer);
        cart.setRestaurant(restaurant);
        cart.setTotalPrice(BigDecimal.valueOf(400.00));
        cart.setExpiresAt(Instant.now().plusSeconds(3600));
        cart.setItems(List.of(cartItem));
    }

    @Test
    @DisplayName("Successfully creates order with dynamic distance-based delivery fee and route info")
    void createOrderWithItems_success() {
        PlaceOrderRequest req = new PlaceOrderRequest(address.getId(), PaymentMethod.COD, BigDecimal.valueOf(10.00));

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(addressRepository.findByIdAndUser(address.getId(), customer)).thenReturn(Optional.of(address));
        when(cartRepository.findByCustomer(customer)).thenReturn(Optional.of(cart));

        // 4200 meters road distance: base 15 + (4.2 * 8 = 33.60) = 48.60 delivery fee
        when(routingGateway.route(any(GeoPoint.class), any(GeoPoint.class)))
                .thenReturn(new RouteResult(4200.0, 720));

        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        Order createdOrder = orderCreationService.createOrderWithItems(customer.getId(), req);

        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.getDeliveryFee()).isEqualByComparingTo(BigDecimal.valueOf(48.60));
        assertThat(createdOrder.getDeliveryDistanceMeters()).isEqualTo(4200.0);
        assertThat(createdOrder.getEstimatedDeliverySeconds()).isEqualTo(720L);
        assertThat(createdOrder.getCurrentStatus()).isEqualTo(OrderStatus.PLACED);

        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(any());
        verify(orderStatusHistoryRepository).save(any());
    }
}
