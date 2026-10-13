package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.cart.exception.CartExpiredException;
import com.quickbite.quickbite.cart.model.Cart;
import com.quickbite.quickbite.cart.model.CartItem;
import com.quickbite.quickbite.cart.repository.CartRepository;
import com.quickbite.quickbite.common.config.property.OrderProperties;
import com.quickbite.quickbite.common.dto.PriceBreakdown;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.routing.RouteResult;
import com.quickbite.quickbite.menu.model.MenuItem;
import com.quickbite.quickbite.order.dto.PlaceOrderRequest;
import com.quickbite.quickbite.order.exception.PriceChangedException;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreationServiceImplTest {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Mock private OrderLifecycleService orderLifecycleService;
    @Mock private UserRepository userRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private CartRepository cartRepository;
    @Mock private OrderPricingService orderPricingService;
    @Mock private OrderTimeoutQueueService orderTimeoutQueueService;

    private OrderProperties orderProperties;
    private OrderCreationServiceImpl orderCreationService;

    private User customer;
    private Address address;
    private Restaurant restaurant;
    private Cart cart;
    private MenuItem menuItem;
    private PriceBreakdown samplePriceBreakdown;

    @BeforeEach
    void setUp() {
        orderProperties = new OrderProperties(15, 100);
        orderCreationService = new OrderCreationServiceImpl(
                userRepository,
                addressRepository,
                cartRepository,
                orderPricingService,
                orderLifecycleService,
                orderTimeoutQueueService,
                orderProperties
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

        samplePriceBreakdown = new PriceBreakdown(
                BigDecimal.valueOf(400.00),
                BigDecimal.ZERO,
                BigDecimal.valueOf(48.60),
                BigDecimal.valueOf(5.00),
                BigDecimal.valueOf(20.00),
                BigDecimal.valueOf(10.00),
                BigDecimal.valueOf(483.60),
                4200.0,
                720L
        );
    }

    @Test
    @DisplayName("Successfully creates order with pricing breakdown and delegates to persistNewOrder")
    void createOrderWithItems_success() {
        PlaceOrderRequest req = new PlaceOrderRequest(address.getId(), PaymentMethod.COD, BigDecimal.valueOf(10.00));

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(addressRepository.findByIdAndUser(address.getId(), customer)).thenReturn(Optional.of(address));
        when(cartRepository.findDetailedByCustomer(customer)).thenReturn(Optional.of(cart));
        when(orderPricingService.calculatePrice(cart, address, BigDecimal.valueOf(10.00)))
                .thenReturn(samplePriceBreakdown);

        when(orderLifecycleService.persistNewOrder(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(i -> {
                    Order o = new Order();
                    o.setId(UUID.randomUUID());
                    o.setDeliveryFee(i.getArgument(4));
                    RouteResult r = i.getArgument(3);
                    o.setDeliveryDistanceMeters(r.distanceMeters());
                    o.setEstimatedDeliverySeconds((long) r.durationSeconds());
                    o.setCurrentStatus(i.getArgument(10));
                    return o;
                });

        Order createdOrder = orderCreationService.createOrderWithItems(customer.getId(), req);

        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.getDeliveryFee()).isEqualByComparingTo(BigDecimal.valueOf(48.60));
        assertThat(createdOrder.getDeliveryDistanceMeters()).isEqualTo(4200.0);
        assertThat(createdOrder.getEstimatedDeliverySeconds()).isEqualTo(720L);
        assertThat(createdOrder.getCurrentStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);

        verify(orderLifecycleService).persistNewOrder(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(orderTimeoutQueueService).scheduleOrderAbandonment(eq(createdOrder.getId()), any(java.time.Duration.class));
    }

    @Test
    @DisplayName("Throws PriceChangedException when expectedTotal does not match currentTotal")
    void createOrderWithItems_priceDrift_throwsPriceChangedException() {
        BigDecimal staleExpectedTotal = BigDecimal.valueOf(450.00); // Actual is 483.60
        PlaceOrderRequest req = new PlaceOrderRequest(address.getId(), PaymentMethod.COD, BigDecimal.valueOf(10.00), staleExpectedTotal);

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(addressRepository.findByIdAndUser(address.getId(), customer)).thenReturn(Optional.of(address));
        when(cartRepository.findDetailedByCustomer(customer)).thenReturn(Optional.of(cart));
        when(orderPricingService.calculatePrice(cart, address, BigDecimal.valueOf(10.00)))
                .thenReturn(samplePriceBreakdown);

        assertThatThrownBy(() -> orderCreationService.createOrderWithItems(customer.getId(), req))
                .isInstanceOf(PriceChangedException.class)
                .satisfies(ex -> {
                    PriceChangedException pce = (PriceChangedException) ex;
                    assertThat(pce.getExpectedTotal()).isEqualTo(staleExpectedTotal);
                    assertThat(pce.getCurrentTotal()).isEqualByComparingTo(BigDecimal.valueOf(483.60));
                    assertThat(pce.getCurrentBreakdown()).isEqualTo(samplePriceBreakdown);
                });
    }

    @Test
    @DisplayName("Throws BadRequestException when cart has no items")
    void createOrderWithItems_emptyCart_throwsBadRequestException() {
        cart.setItems(Collections.emptyList());
        PlaceOrderRequest req = new PlaceOrderRequest(address.getId(), PaymentMethod.COD, BigDecimal.ZERO);

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(addressRepository.findByIdAndUser(address.getId(), customer)).thenReturn(Optional.of(address));
        when(cartRepository.findDetailedByCustomer(customer)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderCreationService.createOrderWithItems(customer.getId(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot place an order with an empty cart");
    }

    @Test
    @DisplayName("Throws CartExpiredException when cart is expired")
    void createOrderWithItems_expiredCart_throwsCartExpiredException() {
        cart.setExpiresAt(Instant.now().minusSeconds(10));
        PlaceOrderRequest req = new PlaceOrderRequest(address.getId(), PaymentMethod.COD, BigDecimal.ZERO);

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(addressRepository.findByIdAndUser(address.getId(), customer)).thenReturn(Optional.of(address));
        when(cartRepository.findDetailedByCustomer(customer)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderCreationService.createOrderWithItems(customer.getId(), req))
                .isInstanceOf(CartExpiredException.class);

        verify(cartRepository).delete(cart);
    }
}
