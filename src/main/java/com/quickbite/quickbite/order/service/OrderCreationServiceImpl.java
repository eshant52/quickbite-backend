package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.cart.exception.CartExpiredException;
import com.quickbite.quickbite.cart.model.Cart;
import com.quickbite.quickbite.cart.repository.CartRepository;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.common.routing.GeoPoint;
import com.quickbite.quickbite.common.routing.RouteResult;
import com.quickbite.quickbite.common.routing.RoutingGateway;
import com.quickbite.quickbite.order.dto.PlaceOrderRequest;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.service.fee.DeliveryFeeCalculator;
import com.quickbite.quickbite.common.config.property.DeliveryFeeProperties;
import com.quickbite.quickbite.order.service.fee.FeeContext;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.AddressRepository;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Handles the first transactional unit of the checkout flow.
 *
 * <p>This bean is intentionally narrow: it validates, builds, and persists
 * the {@link Order} aggregate (order rows + item snapshots + initial status
 * history) in a single short transaction that commits and
 * releases the DB connection before any payment gateway call is made.
 *
 * <p>External routing and fee calculations are executed <em>outside</em> the database
 * transaction boundary to prevent HikariCP connection checkout starvation.
 */
@Service
public class OrderCreationServiceImpl implements OrderCreationService {

    private static final BigDecimal PLATFORM_FEE = BigDecimal.valueOf(5.00).setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal GST_RATE = BigDecimal.valueOf(0.05);

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final RoutingGateway routingGateway;
    private final List<DeliveryFeeCalculator> feeCalculators;
    private final DeliveryFeeProperties feeProperties;
    private final OrderLifecycleService orderLifecycleService;

    @Autowired
    public OrderCreationServiceImpl(
            UserRepository userRepository,
            AddressRepository addressRepository,
            CartRepository cartRepository,
            RoutingGateway routingGateway,
            List<DeliveryFeeCalculator> feeCalculators,
            DeliveryFeeProperties feeProperties,
            OrderLifecycleService orderLifecycleService) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.cartRepository = cartRepository;
        this.routingGateway = routingGateway;
        this.feeCalculators = feeCalculators;
        this.feeProperties = feeProperties;
        this.orderLifecycleService = orderLifecycleService;
    }

    /**
     * TX 1 of the checkout flow — validates, creates, and commits the Order.
     *
     * <p>Transaction scope: starts on entry, commits on return, releasing the
     * DB connection before payment initiation (TX 2) begins.
     */
    @Override
    public Order createOrderWithItems(UUID customerId, PlaceOrderRequest req) {

        // 1. Load entities
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Address customerAddress = addressRepository.findByIdAndUser(req.addressId(), customer)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery address not found"));

        Cart cart = cartRepository.findByCustomer(customer)
                .orElseThrow(() -> new ResourceNotFoundException("Active cart not found"));


        // 2. Guard checks
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot place an order with an empty cart");
        }
        if (Instant.now().isAfter(cart.getExpiresAt())) {
            cartRepository.delete(cart);
            throw new CartExpiredException("Your cart has expired. Please add items again.");
        }


        // 3. Compute route via the profile-configured RoutingGateway
        RouteResult route = computeRoute(cart.getRestaurant().getAddress(), customerAddress);


        // 4. Fee calculation via Chain of Responsibility
        GeoPoint restaurantPoint = toGeoPoint(cart.getRestaurant().getAddress().getLocation());
        GeoPoint customerPoint = toGeoPoint(customerAddress.getLocation());
        FeeContext feeContext = new FeeContext(restaurantPoint, customerPoint, route);

        BigDecimal computedFee = BigDecimal.ZERO;
        for (DeliveryFeeCalculator calculator : feeCalculators) {
            computedFee = calculator.calculate(feeContext, computedFee);
        }


        // 5. Apply min/max caps
        final BigDecimal deliveryFee = computedFee.max(feeProperties.minFee())
                .min(feeProperties.maxFee())
                .setScale(2, RoundingMode.HALF_UP);


        // 6. Final order totals
        BigDecimal subTotal = cart.getTotalPrice();
        BigDecimal taxAmount = subTotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tip = req.tipAmount() != null
                ? req.tipAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subTotal.add(deliveryFee)
                .add(PLATFORM_FEE)
                .add(taxAmount).add(tip);


        // 7. Initial order status
        OrderStatus initialStatus = OrderStatus.AWAITING_PAYMENT;

        // 8. Persist Order in a short, isolated transaction
        return orderLifecycleService.persistNewOrder(
                customer,
                customerAddress,
                cart,
                route,
                deliveryFee,
                PLATFORM_FEE,
                subTotal,
                taxAmount,
                tip,
                total,
                initialStatus
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RouteResult computeRoute(Address restaurantAddress, Address customerAddress) {
        if (restaurantAddress == null || restaurantAddress.getLocation() == null
                || customerAddress == null || customerAddress.getLocation() == null) {
            // Fallback: zero-distance route if coordinates are missing
            return new RouteResult(0.0, 0L);
        }
        GeoPoint from = toGeoPoint(restaurantAddress.getLocation());
        GeoPoint to = toGeoPoint(customerAddress.getLocation());
        return routingGateway.route(from, to);
    }

    private GeoPoint toGeoPoint(Point point) {
        return GeoPoint.of(point.getY(), point.getX());  // JTS: X=lng, Y=lat
    }
}
