package com.quickbite.quickbite.cart.service;

import com.quickbite.quickbite.cart.dto.AddCartItemRequest;
import com.quickbite.quickbite.cart.dto.CartResponse;
import com.quickbite.quickbite.cart.dto.UpdateCartItemRequest;
import com.quickbite.quickbite.cart.exception.CartConflictException;
import com.quickbite.quickbite.cart.model.Cart;
import com.quickbite.quickbite.cart.model.CartItem;
import com.quickbite.quickbite.cart.repository.CartItemRepository;
import com.quickbite.quickbite.cart.repository.CartRepository;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.menu.exception.MenuItemNotFoundException;
import com.quickbite.quickbite.menu.model.MenuItem;
import com.quickbite.quickbite.menu.repository.MenuItemRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final MenuItemRepository menuItemRepository;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           UserRepository userRepository,
                           MenuItemRepository menuItemRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Override
    @Transactional
    public CartResponse getCart(UUID customerId) {
        User customer = loadCustomer(customerId);
        Cart cart = findActiveCart(customer)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        return CartResponse.from(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(UUID customerId, AddCartItemRequest req) {
        User customer = loadCustomer(customerId);
        MenuItem menuItem = menuItemRepository.findById(req.menuItemId())
                .orElseThrow(() -> new MenuItemNotFoundException("Menu item not found"));

        if (!menuItem.isAvailable()) {
            throw new BadRequestException("This menu item is not available for ordering");
        }

        // 1. Fetch only ACTIVE (non-expired) cart
        Optional<Cart> existingCartOpt = findActiveCart(customer);

        if (existingCartOpt.isPresent()) {
            Cart existingCart = existingCartOpt.get();

            // 2. Validate restaurant
            if (!existingCart.getRestaurant().getId().equals(menuItem.getRestaurant().getId())) {
                throw new CartConflictException("Your cart has items from \"" +
                        existingCart.getRestaurant().getName() +
                        "\". Clear your cart to order from a different restaurant.");
            }

            // 3. Update existing item or add new item
            Optional<CartItem> existingItemOpt = cartItemRepository.findByCartAndMenuItem(existingCart, menuItem);

            if (existingItemOpt.isPresent()) {
                CartItem existingItem = existingItemOpt.get();
                existingItem.setQuantity(existingItem.getQuantity() + req.quantity());
                existingItem.setSubTotal(existingItem.getUnitPrice()
                        .multiply(BigDecimal.valueOf(existingItem.getQuantity())));
                cartItemRepository.save(existingItem);
            } else {
                createCartItem(req, menuItem, existingCart);
            }

            return CartResponse.from(recalculate(existingCart));
        }

        // 4. If no active cart exists (or previous expired one was deleted), create new
        Cart newCart = new Cart();
        newCart.setCustomer(customer);
        newCart.setRestaurant(menuItem.getRestaurant());
        newCart.setTotalPrice(BigDecimal.ZERO);
        newCart.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        Cart savedCart = cartRepository.save(newCart);

        createCartItem(req, menuItem, savedCart);

        return CartResponse.from(recalculate(savedCart));
    }

    @Override
    @Transactional
    public CartResponse updateItem(UUID customerId, UUID cartItemId, UpdateCartItemRequest req) {
        User customer = loadCustomer(customerId);
        Cart cart = findActiveCart(customer)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem cartItem = cartItemRepository.findByIdAndCart(cartItemId, cart)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        cartItem.setQuantity(req.quantity());
        cartItem.setSubTotal(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(req.quantity())));
        cartItemRepository.save(cartItem);

        return CartResponse.from(recalculate(cart));
    }

    @Override
    @Transactional
    public void removeItem(UUID customerId, UUID cartItemId) {
        User customer = loadCustomer(customerId);
        Cart cart = findActiveCart(customer)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem cartItem = cartItemRepository.findByIdAndCart(cartItemId, cart)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            cartRepository.delete(cart);
        } else {
            recalculate(cart);
        }
    }

    @Override
    @Transactional
    public void clearCart(UUID customerId) {
        User customer = loadCustomer(customerId);
        cartRepository.findByCustomer(customer).ifPresent(cartRepository::delete);
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private User loadCustomer(UUID customerId) {
        return userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Finds the cart for a user and verifies freshness.
     * If expired, automatically deletes the stale cart from the database.
     */
    private Optional<Cart> findActiveCart(User customer) {
        return cartRepository.findByCustomer(customer).flatMap(cart -> {
            if (Instant.now().isAfter(cart.getExpiresAt())) {
                cartRepository.delete(cart);
                return Optional.empty();
            }
            return Optional.of(cart);
        });
    }

    private Cart recalculate(Cart cart) {
        BigDecimal total = (cart.getItems() != null)
                ? cart.getItems().stream()
                    .map(CartItem::getSubTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        cart.setTotalPrice(total);
        cart.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        return cartRepository.save(cart);
    }

    private void createCartItem(AddCartItemRequest req, MenuItem menuItem, Cart cart) {
        CartItem newItem = new CartItem();
        newItem.setMenuItem(menuItem);
        newItem.setQuantity(req.quantity());
        newItem.setUnitPrice(menuItem.getPrice());
        newItem.setSubTotal(menuItem.getPrice().multiply(BigDecimal.valueOf(req.quantity())));

        cart.addItem(newItem);
        cartItemRepository.save(newItem);
    }
}
