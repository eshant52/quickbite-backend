package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.cart.exception.CartExpiredException;
import com.quickbite.quickbite.cart.model.Cart;
import com.quickbite.quickbite.cart.repository.CartRepository;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.order.dto.PlaceOrderRequest;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderItem;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.model.OrderStatusHistory;
import com.quickbite.quickbite.order.repository.OrderItemRepository;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.order.repository.OrderStatusHistoryRepository;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.AddressRepository;
import com.quickbite.quickbite.user.repository.UserRepository;
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

    // Fixed fees — move to @ConfigurationProperties when they need to vary
    private static final BigDecimal DELIVERY_FEE   = BigDecimal.valueOf(30.00).setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal PLATFORM_FEE   = BigDecimal.valueOf(5.00).setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal GST_RATE        = BigDecimal.valueOf(0.05);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;

    public OrderCreationServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            UserRepository userRepository,
            AddressRepository addressRepository,
            CartRepository cartRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.cartRepository = cartRepository;
    }

    /**
     * TX 1 of the checkout flow — validates, creates, and commits the Order.
     *
     * <p>Transaction scope: starts on entry, commits on return, releasing the
     * DB connection before payment initiation (TX 2) begins. No external HTTP
     * calls are made here.
     */
    @Override
    @Transactional
    public Order createOrderWithItems(UUID customerId, PlaceOrderRequest req) {

        // ── 1. Load entities ────────────────────────────────────────────────
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Address address = addressRepository.findByIdAndUser(req.addressId(), customer)
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

        // ── 3. Fee calculation ───────────────────────────────────────────────
        BigDecimal subTotal   = cart.getTotalPrice();
        BigDecimal taxAmount  = subTotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tip        = req.tipAmount() != null
                ? req.tipAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total      = subTotal.add(DELIVERY_FEE).add(PLATFORM_FEE).add(taxAmount).add(tip);

        // ── 4. Initial order status ──────────────────────────────────────────
        // COD        → PLACED immediately (no gateway handshake needed)
        // Online     → AWAITING_PAYMENT until the gateway webhook confirms
        OrderStatus initialStatus = req.paymentMethod().isOnline()
                ? OrderStatus.AWAITING_PAYMENT
                : OrderStatus.PLACED;

        // ── 5. Persist Order ─────────────────────────────────────────────────
        Order order = new Order();
        order.setCustomer(customer);
        order.setRestaurant(cart.getRestaurant());
        order.setDeliveryAddress(formatAddress(address));
        order.setDeliveryLocation(address.getLocation());
        order.setSubtotal(subTotal);
        order.setDiscountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        order.setDeliveryFee(DELIVERY_FEE);
        order.setPlatformFee(PLATFORM_FEE);
        order.setTaxAmount(taxAmount);
        order.setTipAmount(tip);
        order.setTotalAmount(total);
        order.setCurrentStatus(initialStatus);

        Order savedOrder = orderRepository.save(order);

        // ── 6. Snapshot cart items as OrderItems ─────────────────────────────
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

        // ── 7. Record initial status history ─────────────────────────────────
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(savedOrder);
        history.setOrderStatus(initialStatus);
        orderStatusHistoryRepository.save(history);

        // Cart is intentionally NOT cleared here.
        // – COD:    cleared by CodPaymentStrategy after the payment record is written.
        // – Online: cleared by the webhook handler once gateway confirms payment.
        return savedOrder;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
