package com.quickbite.quickbite.cart.service;

import com.quickbite.quickbite.cart.dto.CheckoutPreviewResponse;
import com.quickbite.quickbite.cart.exception.CartExpiredException;
import com.quickbite.quickbite.cart.model.Cart;
import com.quickbite.quickbite.cart.model.CartItem;
import com.quickbite.quickbite.cart.repository.CartRepository;
import com.quickbite.quickbite.common.dto.PriceBreakdown;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.order.service.OrderPricingService;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.AddressRepository;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class CartCheckoutServiceImpl implements CartCheckoutService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final OrderPricingService orderPricingService;

    @Autowired
    public CartCheckoutServiceImpl(
            UserRepository userRepository,
            AddressRepository addressRepository,
            CartRepository cartRepository,
            OrderPricingService orderPricingService) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.cartRepository = cartRepository;
        this.orderPricingService = orderPricingService;
    }

    /**
     * Deliberately NOT {@code @Transactional}.
     *
     * <p>Pricing performs external I/O (Redis route cache + routing provider HTTP call). Each
     * repository call below runs in its own short transaction and releases the DB connection
     * on return; the cart is loaded with an entity graph so the pricing step needs no DB access.
     * A side benefit: the expired-cart {@code delete} commits independently instead of being
     * rolled back by the {@link CartExpiredException} thrown right after it.
     */
    @Override
    public CheckoutPreviewResponse getCheckoutPreview(UUID customerId, UUID addressId, BigDecimal tipAmount) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Address customerAddress = addressRepository.findByIdAndUser(addressId, customer)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery address not found"));

        Cart cart = cartRepository.findDetailedByCustomer(customer)
                .orElseThrow(() -> new ResourceNotFoundException("Active cart not found"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot checkout with an empty cart");
        }

        if (Instant.now().isAfter(cart.getExpiresAt())) {
            cartRepository.delete(cart);
            throw new CartExpiredException("Your cart has expired. Please add items again.");
        }

        for (CartItem item : cart.getItems()) {
            if (item.getMenuItem() != null && !item.getMenuItem().isAvailable()) {
                throw new BadRequestException("Item '" + item.getMenuItem().getName() + "' is no longer available");
            }
        }

        PriceBreakdown priceBreakdown = orderPricingService.calculatePrice(cart, customerAddress, tipAmount);

        return CheckoutPreviewResponse.from(cart, priceBreakdown);
    }
}
