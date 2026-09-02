package com.quickbite.quickbite.delivery.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.common.event.order.OrderStatusChangedEvent;
import com.quickbite.quickbite.common.event.payment.PaymentStatusChangedEvent;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.quickbite.delivery.dto.UpdateLocationRequest;
import com.quickbite.quickbite.delivery.exception.DeliveryAgentNotFoundException;
import com.quickbite.quickbite.delivery.exception.NoAvailableDeliveryAgentException;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.model.DeliveryAgentVerificationStatus;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentVerificationHistoryRepository;
import com.quickbite.quickbite.delivery.service.strategy.DeliveryAssignmentStrategy;
import com.quickbite.quickbite.order.dto.OrderResponse;
import com.quickbite.quickbite.order.exception.OrderNotFoundException;
import com.quickbite.quickbite.order.exception.OrderStateException;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.model.OrderStatusHistory;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.order.repository.OrderStatusHistoryRepository;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;
import com.quickbite.quickbite.payment.model.PaymentStatusHistory;
import com.quickbite.quickbite.payment.repository.PaymentRepository;
import com.quickbite.quickbite.payment.repository.PaymentStatusHistoryRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DeliveryServiceImpl implements DeliveryService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final DeliveryAgentRepository deliveryAgentRepository;
    private final DeliveryAgentVerificationHistoryRepository verificationHistoryRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentStatusHistoryRepository paymentStatusHistoryRepository;
    private final DeliveryAssignmentStrategy deliveryAssignmentStrategy;
    private final ApplicationEventPublisher eventPublisher;

    public DeliveryServiceImpl(
            DeliveryAgentRepository deliveryAgentRepository,
            DeliveryAgentVerificationHistoryRepository verificationHistoryRepository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            PaymentRepository paymentRepository,
            PaymentStatusHistoryRepository paymentStatusHistoryRepository,
            DeliveryAssignmentStrategy deliveryAssignmentStrategy,
            ApplicationEventPublisher eventPublisher
    ) {
        this.deliveryAgentRepository = deliveryAgentRepository;
        this.verificationHistoryRepository = verificationHistoryRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.paymentRepository = paymentRepository;
        this.paymentStatusHistoryRepository = paymentStatusHistoryRepository;
        this.deliveryAssignmentStrategy = deliveryAssignmentStrategy;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAgentResponse getMyProfile(UUID userId) {
        User user = loadUser(userId);
        DeliveryAgent agent = deliveryAgentRepository.findByUser(user)
                .orElseThrow(() -> new DeliveryAgentNotFoundException("Delivery agent profile not found"));

        return DeliveryAgentResponse.from(agent);
    }

    @Override
    public DeliveryAgentResponse updateLocation(UUID userId, UpdateLocationRequest req) {
        User user = loadUser(userId);
        DeliveryAgent agent = deliveryAgentRepository.findByUser(user)
                .orElseThrow(() -> new DeliveryAgentNotFoundException("Delivery agent profile not found"));

        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(req.longitude(), req.latitude()));
        agent.setLastLocation(location);

        if (agent.getCurrentStatus() == DeliveryAgentVerificationStatus.APPROVED) {
            agent.setAvailable(true);
        }

        DeliveryAgent saved = deliveryAgentRepository.save(agent);
        return DeliveryAgentResponse.from(saved);
    }

    @Override
    public void autoAssign(Order order) {
        DeliveryAgent agent = deliveryAssignmentStrategy.findAgent(order)
                .orElseThrow(() -> new NoAvailableDeliveryAgentException("No delivery agent available near order location"));

        order.setDeliveryAgent(agent);
        orderRepository.save(order);

        agent.setAvailable(false);
        deliveryAgentRepository.save(agent);
    }

    @Override
    public OrderResponse markOutForDelivery(UUID orderId, UUID agentUserId) {
        User user = loadUser(agentUserId);
        DeliveryAgent agent = deliveryAgentRepository.findByUser(user)
                .orElseThrow(() -> new DeliveryAgentNotFoundException("Delivery agent profile not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (order.getDeliveryAgent() == null || !order.getDeliveryAgent().getId().equals(agent.getId())) {
            throw new BadRequestException("Order is not assigned to you");
        }

        if (order.getCurrentStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new OrderStateException("Order must be in READY_FOR_PICKUP state to mark OUT_FOR_DELIVERY. Current status: " + order.getCurrentStatus());
        }

        order.setCurrentStatus(OrderStatus.OUT_FOR_DELIVERY);
        Order savedOrder = orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(savedOrder);
        history.setOrderStatus(OrderStatus.OUT_FOR_DELIVERY);
        orderStatusHistoryRepository.save(history);

        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                savedOrder.getId(),
                savedOrder.getCustomer().getId(),
                savedOrder.getRestaurant().getId(),
                OrderStatus.READY_FOR_PICKUP,
                OrderStatus.OUT_FOR_DELIVERY,
                Instant.now()
        ));

        return OrderResponse.from(savedOrder);
    }

    @Override
    public OrderResponse markDelivered(UUID orderId, UUID agentUserId) {
        User user = loadUser(agentUserId);
        DeliveryAgent agent = deliveryAgentRepository.findByUser(user)
                .orElseThrow(() -> new DeliveryAgentNotFoundException("Delivery agent profile not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (order.getDeliveryAgent() == null || !order.getDeliveryAgent().getId().equals(agent.getId())) {
            throw new BadRequestException("Order is not assigned to you");
        }

        if (order.getCurrentStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new OrderStateException("Order must be in OUT_FOR_DELIVERY state to mark DELIVERED. Current status: " + order.getCurrentStatus());
        }

        order.setCurrentStatus(OrderStatus.DELIVERED);
        Order savedOrder = orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(savedOrder);
        history.setOrderStatus(OrderStatus.DELIVERED);
        orderStatusHistoryRepository.save(history);

        // Free agent to receive new assignments
        agent.setAvailable(true);
        deliveryAgentRepository.save(agent);

        // Handle Cash on Delivery payment collection
        Payment payment = paymentRepository.findByOrderId(savedOrder.getId()).orElse(null);
        if (payment != null && payment.getPaymentMethod() == PaymentMethod.COD && payment.getCurrentStatus() == PaymentStatus.PENDING) {
            payment.setCurrentStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);

            PaymentStatusHistory paymentHistory = new PaymentStatusHistory();
            paymentHistory.setPayment(payment);
            paymentHistory.setStatus(PaymentStatus.SUCCESS);
            paymentStatusHistoryRepository.save(paymentHistory);

            eventPublisher.publishEvent(new PaymentStatusChangedEvent(
                    payment.getId(),
                    savedOrder.getId(),
                    savedOrder.getCustomer().getId(),
                    PaymentStatus.PENDING,
                    PaymentStatus.SUCCESS,
                    PaymentMethod.COD,
                    payment.getAmount(),
                    Instant.now()
            ));
        }

        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                savedOrder.getId(),
                savedOrder.getCustomer().getId(),
                savedOrder.getRestaurant().getId(),
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.DELIVERED,
                Instant.now()
        ));

        return OrderResponse.from(savedOrder);
    }

    @Override
    public DeliveryAgentResponse approveAgent(UUID agentId, UUID adminId) {
        User admin = loadUser(adminId);
        DeliveryAgent agent = deliveryAgentRepository.findById(agentId)
                .orElseThrow(() -> new DeliveryAgentNotFoundException("Delivery agent not found: " + agentId));

        agent.setCurrentStatus(DeliveryAgentVerificationStatus.APPROVED);
        if (agent.getLastLocation() != null) {
            agent.setAvailable(true);
        }
        DeliveryAgent saved = deliveryAgentRepository.save(agent);

        return DeliveryAgentResponse.from(saved);
    }

    @Override
    public DeliveryAgentResponse rejectAgent(UUID agentId, UUID adminId, String remarks) {
        User admin = loadUser(adminId);
        DeliveryAgent agent = deliveryAgentRepository.findById(agentId)
                .orElseThrow(() -> new DeliveryAgentNotFoundException("Delivery agent not found: " + agentId));

        agent.setCurrentStatus(DeliveryAgentVerificationStatus.REJECTED);
        agent.setAvailable(false);
        DeliveryAgent saved = deliveryAgentRepository.save(agent);

        return DeliveryAgentResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<DeliveryAgentResponse> listAgentsByStatus(
            DeliveryAgentVerificationStatus status,
            UUID cursor,
            int size
    ) {
        int pageSize = Math.clamp(size, 1, 100);
        List<DeliveryAgent> agents = deliveryAgentRepository.findWithCursor(status, cursor, Limit.of(pageSize + 1));

        return CursorPage.of(
                agents.stream().map(DeliveryAgentResponse::from).toList(),
                pageSize,
                DeliveryAgentResponse::id
        );
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
