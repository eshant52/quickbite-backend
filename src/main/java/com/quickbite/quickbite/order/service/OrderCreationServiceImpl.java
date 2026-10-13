package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.cart.exception.CartExpiredException;
import com.quickbite.quickbite.cart.model.Cart;
import com.quickbite.quickbite.cart.repository.CartRepository;
import com.quickbite.quickbite.common.dto.PriceBreakdown;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.common.routing.RouteResult;
import com.quickbite.quickbite.order.dto.PlaceOrderRequest;
import com.quickbite.quickbite.order.exception.PriceChangedException;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.AddressRepository;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.quickbite.quickbite.common.config.property.OrderProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Handles the first transactional unit of the checkout flow.
 *
 * <p>Validates cart and delivery address, computes full pricing via {@link OrderPricingService},
 * asserts against any client price drift ({@code expectedTotal}), and persists the
 * {@link Order} aggregate in a short transaction before payment initiation.
 */
@Service
public class OrderCreationServiceImpl implements OrderCreationService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final OrderPricingService orderPricingService;
    private final OrderLifecycleService orderLifecycleService;
    private final OrderTimeoutQueueService orderTimeoutQueueService;
    private final OrderProperties orderProperties;

    @Autowired
    public OrderCreationServiceImpl(
            UserRepository userRepository,
            AddressRepository addressRepository,
            CartRepository cartRepository,
            OrderPricingService orderPricingService,
            OrderLifecycleService orderLifecycleService,
            OrderTimeoutQueueService orderTimeoutQueueService,
            OrderProperties orderProperties) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.cartRepository = cartRepository;
        this.orderPricingService = orderPricingService;
        this.orderLifecycleService = orderLifecycleService;
        this.orderTimeoutQueueService = orderTimeoutQueueService;
        this.orderProperties = orderProperties;
    }

    /**
     * TX 1 of the checkout flow — validates, prices, checks drift, creates, and commits the Order.
     */
    @Override
    public Order createOrderWithItems(UUID customerId, PlaceOrderRequest req) {

        // 1. Load entities
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Address customerAddress = addressRepository.findByIdAndUser(req.addressId(), customer)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery address not found"));

        Cart cart = cartRepository.findDetailedByCustomer(customer)
                .orElseThrow(() -> new ResourceNotFoundException("Active cart not found"));

        // 2. Guard checks
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot place an order with an empty cart");
        }
        if (Instant.now().isAfter(cart.getExpiresAt())) {
            cartRepository.delete(cart);
            throw new CartExpiredException("Your cart has expired. Please add items again.");
        }

        // 3. Price calculation via OrderPricingService
        PriceBreakdown pricing = orderPricingService.calculatePrice(cart, customerAddress, req.tipAmount());

        // 4. Price-drift protection
        if (req.expectedTotal() != null && pricing.totalAmount().compareTo(req.expectedTotal()) != 0) {
            throw new PriceChangedException(pricing.totalAmount(), req.expectedTotal(), pricing);
        }

        // 5. Initial order status
        OrderStatus initialStatus = OrderStatus.AWAITING_PAYMENT;

        // 6. Persist Order in a short, isolated transaction
        RouteResult route = new RouteResult(pricing.deliveryDistanceMeters(), pricing.estimatedDeliverySeconds());
        Order savedOrder = orderLifecycleService.persistNewOrder(
                customer,
                customerAddress,
                cart,
                route,
                pricing.deliveryFee(),
                pricing.platformFee(),
                pricing.subTotal(),
                pricing.taxAmount(),
                pricing.tipAmount(),
                pricing.totalAmount(),
                initialStatus
        );

        // 7. Schedule abandonment timeout in Redis reliable queue (persistNewOrder has already committed)
        orderTimeoutQueueService.scheduleOrderAbandonment(
                savedOrder.getId(),
                Duration.ofMinutes(orderProperties.abandonTtlMinutes())
        );

        return savedOrder;
    }
}
