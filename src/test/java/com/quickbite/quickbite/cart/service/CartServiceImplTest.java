package com.quickbite.quickbite.cart.service;

import com.quickbite.quickbite.cart.dto.AddCartItemRequest;
import com.quickbite.quickbite.cart.dto.CartResponse;
import com.quickbite.quickbite.cart.model.Cart;
import com.quickbite.quickbite.cart.model.CartItem;
import com.quickbite.quickbite.cart.repository.CartItemRepository;
import com.quickbite.quickbite.cart.repository.CartRepository;
import com.quickbite.quickbite.menu.model.MenuItem;
import com.quickbite.quickbite.menu.repository.MenuItemRepository;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private MenuItemRepository menuItemRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private User customer;
    private Restaurant restaurant1;
    private Restaurant restaurant2;
    private MenuItem item1;
    private MenuItem item2;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(UUID.randomUUID());

        restaurant1 = new Restaurant();
        restaurant1.setId(UUID.randomUUID());
        restaurant1.setName("Pizza Place");

        restaurant2 = new Restaurant();
        restaurant2.setId(UUID.randomUUID());
        restaurant2.setName("Burger Joint");

        item1 = new MenuItem();
        item1.setId(UUID.randomUUID());
        item1.setName("Margherita");
        item1.setPrice(BigDecimal.valueOf(250.00));
        item1.setAvailable(true);
        item1.setRestaurant(restaurant1);

        item2 = new MenuItem();
        item2.setId(UUID.randomUUID());
        item2.setName("Cheeseburger");
        item2.setPrice(BigDecimal.valueOf(180.00));
        item2.setAvailable(true);
        item2.setRestaurant(restaurant2);
    }

    @Test
    @DisplayName("Adding item when no active cart exists creates new cart")
    void addItem_noExistingCart_createsNew() {
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(menuItemRepository.findById(item1.getId())).thenReturn(Optional.of(item1));
        when(cartRepository.findDetailedByCustomer(customer)).thenReturn(Optional.empty());

        Cart savedCart = new Cart();
        savedCart.setId(UUID.randomUUID());
        savedCart.setCustomer(customer);
        savedCart.setRestaurant(restaurant1);
        savedCart.setTotalPrice(BigDecimal.ZERO);
        savedCart.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));

        when(cartRepository.save(any(Cart.class))).thenReturn(savedCart);
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartResponse response = cartService.addItem(customer.getId(), new AddCartItemRequest(item1.getId(), 2));

        assertThat(response).isNotNull();
        assertThat(response.restaurantId()).isEqualTo(restaurant1.getId());
        verify(cartRepository, atLeastOnce()).save(any(Cart.class));
    }

    @Test
    @DisplayName("Adding item from a different restaurant auto-clears existing cart and creates new one (Requirement #6)")
    void addItem_differentRestaurant_clearsExistingCart() {
        Cart existingCart = new Cart();
        existingCart.setId(UUID.randomUUID());
        existingCart.setCustomer(customer);
        existingCart.setRestaurant(restaurant1);
        existingCart.setTotalPrice(BigDecimal.valueOf(500.00));
        existingCart.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));

        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(menuItemRepository.findById(item2.getId())).thenReturn(Optional.of(item2)); // Item from restaurant2
        when(cartRepository.findDetailedByCustomer(customer)).thenReturn(Optional.of(existingCart));

        Cart newCart = new Cart();
        newCart.setId(UUID.randomUUID());
        newCart.setCustomer(customer);
        newCart.setRestaurant(restaurant2);
        newCart.setTotalPrice(BigDecimal.ZERO);
        newCart.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));

        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartResponse response = cartService.addItem(customer.getId(), new AddCartItemRequest(item2.getId(), 1));

        assertThat(response).isNotNull();
        assertThat(response.restaurantId()).isEqualTo(restaurant2.getId());
        // Verify previous cart was deleted and flushed
        verify(cartRepository).delete(existingCart);
        verify(cartRepository).flush();
        verify(cartRepository, atLeastOnce()).save(any(Cart.class));
    }
}
