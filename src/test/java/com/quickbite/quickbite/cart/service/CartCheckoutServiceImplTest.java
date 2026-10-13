package com.quickbite.quickbite.cart.service;

import com.quickbite.quickbite.cart.dto.CheckoutPreviewResponse;
import com.quickbite.quickbite.cart.exception.CartExpiredException;
import com.quickbite.quickbite.cart.model.Cart;
import com.quickbite.quickbite.cart.model.CartItem;
import com.quickbite.quickbite.cart.repository.CartRepository;
import com.quickbite.quickbite.common.dto.PriceBreakdown;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.menu.model.MenuItem;
import com.quickbite.quickbite.order.service.OrderPricingService;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.AddressRepository;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartCheckoutServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private CartRepository cartRepository;
    @Mock private OrderPricingService orderPricingService;

    private CartCheckoutServiceImpl checkoutService;

    private User customer;
    private Address address;
    private Restaurant restaurant;
    private Cart cart;
    private MenuItem menuItem;
    private PriceBreakdown priceBreakdown;

    @BeforeEach
    void setUp() {
        checkoutService = new CartCheckoutServiceImpl(
                userRepository,
                addressRepository,
                cartRepository,
                orderPricingService
        );

        customer = new User();
        customer.setId(UUID.randomUUID());

        address = new Address();
        address.setId(UUID.randomUUID());
        address.setUser(customer);

        restaurant = new Restaurant();
        restaurant.setId(UUID.randomUUID());
        restaurant.setName("Pasta Villa");

        menuItem = new MenuItem();
        menuItem.setId(UUID.randomUUID());
        menuItem.setName("Fettuccine Alfredo");
        menuItem.setPrice(BigDecimal.valueOf(250.00));
        menuItem.setAvailable(true);

        CartItem cartItem = new CartItem();
        cartItem.setMenuItem(menuItem);
        cartItem.setQuantity(2);
        cartItem.setUnitPrice(BigDecimal.valueOf(250.00));
        cartItem.setSubTotal(BigDecimal.valueOf(500.00));

        cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setCustomer(customer);
        cart.setRestaurant(restaurant);
        cart.setTotalPrice(BigDecimal.valueOf(500.00));
        cart.setExpiresAt(Instant.now().plusSeconds(1800));
        cart.setItems(List.of(cartItem));

        priceBreakdown = new PriceBreakdown(
                BigDecimal.valueOf(500.00),
                BigDecimal.ZERO,
                BigDecimal.valueOf(35.00),
                BigDecimal.valueOf(5.00),
                BigDecimal.valueOf(25.00),
                BigDecimal.valueOf(15.00),
                BigDecimal.valueOf(580.00),
                2500.0,
                500L
        );
    }

    @Test
    @DisplayName("Returns full checkout preview including items, restaurant, and price breakdown")
    void getCheckoutPreview_success() {
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(addressRepository.findByIdAndUser(address.getId(), customer)).thenReturn(Optional.of(address));
        when(cartRepository.findDetailedByCustomer(customer)).thenReturn(Optional.of(cart));
        when(orderPricingService.calculatePrice(cart, address, BigDecimal.valueOf(15.00))).thenReturn(priceBreakdown);

        CheckoutPreviewResponse response = checkoutService.getCheckoutPreview(
                customer.getId(), address.getId(), BigDecimal.valueOf(15.00));

        assertThat(response).isNotNull();
        assertThat(response.cartId()).isEqualTo(cart.getId());
        assertThat(response.restaurantId()).isEqualTo(restaurant.getId());
        assertThat(response.restaurantName()).isEqualTo("Pasta Villa");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).menuItemName()).isEqualTo("Fettuccine Alfredo");
        assertThat(response.priceBreakdown()).isEqualTo(priceBreakdown);
    }

    @Test
    @DisplayName("Throws BadRequestException when cart is empty")
    void getCheckoutPreview_emptyCart_throwsBadRequestException() {
        cart.setItems(Collections.emptyList());

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(addressRepository.findByIdAndUser(address.getId(), customer)).thenReturn(Optional.of(address));
        when(cartRepository.findDetailedByCustomer(customer)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> checkoutService.getCheckoutPreview(customer.getId(), address.getId(), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot checkout with an empty cart");
    }

    @Test
    @DisplayName("Throws CartExpiredException and deletes cart when cart is expired")
    void getCheckoutPreview_expiredCart_throwsCartExpiredException() {
        cart.setExpiresAt(Instant.now().minusSeconds(30));

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(addressRepository.findByIdAndUser(address.getId(), customer)).thenReturn(Optional.of(address));
        when(cartRepository.findDetailedByCustomer(customer)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> checkoutService.getCheckoutPreview(customer.getId(), address.getId(), null))
                .isInstanceOf(CartExpiredException.class);

        verify(cartRepository).delete(cart);
    }

    @Test
    @DisplayName("Throws BadRequestException when any item in the cart is no longer available")
    void getCheckoutPreview_unavailableItem_throwsBadRequestException() {
        menuItem.setAvailable(false);

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(addressRepository.findByIdAndUser(address.getId(), customer)).thenReturn(Optional.of(address));
        when(cartRepository.findDetailedByCustomer(customer)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> checkoutService.getCheckoutPreview(customer.getId(), address.getId(), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is no longer available");
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when delivery address does not belong to user")
    void getCheckoutPreview_invalidAddress_throwsResourceNotFoundException() {
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(addressRepository.findByIdAndUser(address.getId(), customer)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkoutService.getCheckoutPreview(customer.getId(), address.getId(), null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Delivery address not found");
    }
}
