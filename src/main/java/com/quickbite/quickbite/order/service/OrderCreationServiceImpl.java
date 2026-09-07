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
import com.quickbite.quickbite.order.model.OrderItem;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.model.OrderStatusHistory;
import com.quickbite.quickbite.order.repository.OrderItemRepository;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.order.repository.OrderStatusHistoryRepository;
import com.quickbite.quickbite.order.service.fee.DeliveryFeeCalculator;
import com.quickbite.quickbite.common.config.property.DeliveryFeeProperties;
import com.quickbite.quickbite.order.service.fee.FeeContext;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.AddressRepository;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * history) in a single {@code @Transactional} boundary that commits and
 * releases the DB connection before any payment gateway call is made.
 *
 * <p>It does <em>not</em> clear the cart, publish events, or call external
 * services — all of which belong to the payment strategy layer.
 */
@Service
public class OrderCreationServiceImpl implements OrderCreationService {

    private static final BigDecimal PLATFORM_FEE = BigDecimal.valueOf(5.00).setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal GST_RATE = BigDecimal.valueOf(0.05);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final RoutingGateway routingGateway;
    private final List<DeliveryFeeCalculator> feeCalculators;
    private final DeliveryFeeProperties feeProperties;

    public OrderCreationServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            UserRepository userRepository,
            AddressRepository addressRepository,
            CartRepository cartRepository,
            RoutingGateway routingGateway,
            List<DeliveryFeeCalculator> feeCalculators,
            DeliveryFeeProperties feeProperties) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.cartRepository = cartRepository;
        this.routingGateway = routingGateway;
        this.feeCalculators = feeCalculators;
        this.feeProperties = feeProperties;
    }

    /**
     * TX 1 of the checkout flow — validates, creates, and commits the Order.
     *
     * <p>Transaction scope: starts on entry, commits on return, releasing the
     * DB connection before payment initiation (TX 2) begins.
     */
    @Override
    @Transactional
    public Order createOrderWithItems(UUID customerId, PlaceOrderRequest req) {

        // ── 1. Load entities ────────────────────────────────────────────────
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Address customerAddress = addressRepository.findByIdAndUser(req.addressId(), customer)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery address not found"));

        Cart cart = cartRepository.findByCustomer(customer)
                .orElseThrow(() -> new ResourceNotFoundException("Active cart not found"));

        // ── 2. Guard checks ─────────────────────────────────────────────────
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot place an order with an empty cart");
        }
        if (Instant.now().isAfter(cart.getExpiresAt())) {
            cartRepository.delete(cart);
            throw new CartExpiredException("Your cart has expired. Please add items again.");
        }

        // ── 3. Compute route via the profile-configured RoutingGateway ───────
        RouteResult route = computeRoute(cart.getRestaurant().getAddress(), customerAddress);

        // ── 4. Fee calculation via Chain of Responsibility ───────────────────
        GeoPoint restaurantPoint = toGeoPoint(cart.getRestaurant().getAddress().getLocation());
        GeoPoint customerPoint = toGeoPoint(customerAddress.getLocation());
        FeeContext feeContext = new FeeContext(restaurantPoint, customerPoint, route);

        BigDecimal deliveryFee = BigDecimal.ZERO;
        for (DeliveryFeeCalculator calculator : feeCalculators) {
            deliveryFee = calculator.calculate(feeContext, deliveryFee);
        }

        // Apply min/max caps
        deliveryFee = deliveryFee.max(feeProperties.minFee()).min(feeProperties.maxFee())
                .setScale(2, RoundingMode.HALF_UP);

        // ── 5. Final order totals ─────────────────────────────────────────────
        BigDecimal subTotal = cart.getTotalPrice();
        BigDecimal taxAmount = subTotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tip = req.tipAmount() != null
                ? req.tipAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subTotal.add(deliveryFee)
                .add(PLATFORM_FEE)
                .add(taxAmount).add(tip);

        // ── 6. Initial order status ──────────────────────────────────────────
        OrderStatus initialStatus = req.paymentMethod().isOnline()
                ? OrderStatus.AWAITING_PAYMENT
                : OrderStatus.PLACED;

        // ── 7. Persist Order ─────────────────────────────────────────────────
        Order order = new Order();
        order.setCustomer(customer);
        order.setRestaurant(cart.getRestaurant());
        order.setDeliveryAddress(formatAddress(customerAddress));
        order.setDeliveryLocation(customerAddress.getLocation());
        order.setSubtotal(subTotal);
        order.setDiscountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        order.setDeliveryFee(deliveryFee);
        order.setPlatformFee(PLATFORM_FEE);
        order.setTaxAmount(taxAmount);
        order.setTipAmount(tip);
        order.setTotalAmount(total);
        order.setCurrentStatus(initialStatus);
        order.setDeliveryDistanceMeters(route.distanceMeters());
        order.setEstimatedDeliverySeconds(route.durationSeconds());

        Order savedOrder = orderRepository.save(order);

        // ── 8. Snapshot cart items as OrderItems ─────────────────────────────
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> {
                    if (!cartItem.getMenuItem().isAvailable()) {
                        throw new BadRequestException(
                                "Item '" + cartItem.getMenuItem().getName() + "' is no longer available");
                    }
                    OrderItem item = new OrderItem();
                    item.setOrder(savedOrder);
                    item.setMenuItem(cartItem.getMenuItem());
                    item.setQuantity(cartItem.getQuantity());
                    item.setUnitPrice(cartItem.getUnitPrice());
                    item.setSubTotal(cartItem.getSubTotal());
                    return item;
                }).toList();

        orderItemRepository.saveAll(orderItems);
        savedOrder.setItems(orderItems);

        // ── 9. Record initial status history ─────────────────────────────────
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(savedOrder);
        history.setOrderStatus(initialStatus);
        orderStatusHistoryRepository.save(history);

        return savedOrder;
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

    private String formatAddress(Address a) {
        StringBuilder sb = new StringBuilder();
        if (a.getHouseNumber() != null && !a.getHouseNumber().isBlank())
            sb.append(a.getHouseNumber()).append(", ");
        if (a.getBuildingName() != null && !a.getBuildingName().isBlank())
            sb.append(a.getBuildingName()).append(", ");
        sb.append(a.getStreet());
        if (a.getLandmark() != null && !a.getLandmark().isBlank())
            sb.append(", Near ").append(a.getLandmark());
        sb.append(", ").append(a.getCity()).append(", ").append(a.getState());
        if (a.getPostalCode() != null && !a.getPostalCode().isBlank())
            sb.append(" - ").append(a.getPostalCode());
        return sb.toString();
    }
}
