package com.quickbite.quickbite.delivery.service;

import com.quickbite.quickbite.common.event.delivery.DeliveryAgentAssignedEvent;
import com.quickbite.quickbite.common.event.delivery.DeliveryDispatchExhaustedEvent;
import com.quickbite.quickbite.common.event.delivery.DeliveryOfferCreatedEvent;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceConflictException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.delivery.exception.DeliveryAgentNotFoundException;
import com.quickbite.quickbite.delivery.exception.OfferExpiredException;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.model.DeliveryDispatchStatus;
import com.quickbite.quickbite.delivery.model.DeliveryOffer;
import com.quickbite.quickbite.delivery.model.DeliveryOfferStatus;
import com.quickbite.quickbite.delivery.model.OrderDispatch;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.delivery.repository.DeliveryOfferRepository;
import com.quickbite.quickbite.delivery.repository.OrderDispatchRepository;
import com.quickbite.quickbite.order.exception.OrderNotFoundException;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.order.service.OrderLifecycleService;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Implements atomic transactional state transitions and event publishing for delivery dispatches.
 * Every method executes in a short, dedicated database transaction.
 */
@Service
public class DeliveryDispatchLifecycleServiceImpl implements DeliveryDispatchLifecycleService {

    private final OrderDispatchRepository orderDispatchRepository;
    private final DeliveryOfferRepository deliveryOfferRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderLifecycleService orderLifecycleService;
    private final ApplicationEventPublisher eventPublisher;

    public DeliveryDispatchLifecycleServiceImpl(
            OrderDispatchRepository orderDispatchRepository,
            DeliveryOfferRepository deliveryOfferRepository,
            DeliveryAgentRepository deliveryAgentRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            OrderLifecycleService orderLifecycleService,
            ApplicationEventPublisher eventPublisher) {
        this.orderDispatchRepository = orderDispatchRepository;
        this.deliveryOfferRepository = deliveryOfferRepository;
        this.deliveryAgentRepository = deliveryAgentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderLifecycleService = orderLifecycleService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Optional<OrderDispatch> createInitialDispatch(UUID orderId, Duration totalDispatchWindow) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        Optional<OrderDispatch> existing = orderDispatchRepository.findByOrderId(orderId);
        if (existing.isPresent() && existing.get().getStatus() != DeliveryDispatchStatus.NOT_STARTED) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        OrderDispatch dispatch = existing.orElseGet(OrderDispatch::new);
        dispatch.setOrder(order);
        dispatch.setStatus(DeliveryDispatchStatus.FINDING_AGENT);
        dispatch.setStartedAt(now);
        dispatch.setCurrentRound(0);
        dispatch.setDispatchDeadline(now.plus(totalDispatchWindow));
        orderDispatchRepository.save(dispatch);
        return Optional.of(dispatch);
    }

    @Override
    @Transactional
    public PendingOfferCheckResult checkAndExpirePendingOffer(UUID orderId) {
        Optional<DeliveryOffer> pendingOfferOpt = deliveryOfferRepository.findByOrderIdAndStatus(
                orderId, DeliveryOfferStatus.PENDING);
        if (pendingOfferOpt.isEmpty()) {
            return PendingOfferCheckResult.noPendingOffer();
        }

        DeliveryOffer pendingOffer = pendingOfferOpt.get();
        Instant now = Instant.now();
        if (!now.isBefore(pendingOffer.getExpiresAt())) {
            pendingOffer.setStatus(DeliveryOfferStatus.EXPIRED);
            pendingOffer.setRespondedAt(now);
            deliveryOfferRepository.save(pendingOffer);
            return PendingOfferCheckResult.expired();
        }

        return PendingOfferCheckResult.active(pendingOffer.getExpiresAt());
    }

    @Override
    @Transactional
    public Optional<OrderDispatch> recordOfferRejection(UUID orderId, UUID agentUserId) {
        DeliveryAgent agent = loadDeliveryAgentForUser(agentUserId);

        Optional<DeliveryOffer> offerOpt = deliveryOfferRepository.findByOrderIdAndAgentIdAndStatus(
                orderId, agent.getId(), DeliveryOfferStatus.PENDING);
        if (offerOpt.isEmpty()) {
            return Optional.empty();
        }

        DeliveryOffer offer = offerOpt.get();
        offer.setStatus(DeliveryOfferStatus.REJECTED);
        offer.setRespondedAt(Instant.now());
        deliveryOfferRepository.save(offer);
        return orderDispatchRepository.findByOrderId(orderId);
    }

    @Override
    @Transactional
    public DeliveryOffer recordCreatedOffer(
            Order order,
            DeliveryAgent agent,
            int roundNumber,
            double radiusKm,
            Instant expiresAt
    ) {
        Instant now = Instant.now();
        DeliveryOffer offer = new DeliveryOffer();
        offer.setOrder(order);
        offer.setAgent(agent);
        offer.setRoundNumber(roundNumber);
        offer.setRadiusKm(BigDecimal.valueOf(radiusKm));
        offer.setStatus(DeliveryOfferStatus.PENDING);
        offer.setOfferedAt(now);
        offer.setExpiresAt(expiresAt);
        deliveryOfferRepository.save(offer);

        orderDispatchRepository.findByOrderId(order.getId()).ifPresent(dispatch -> {
            dispatch.setCurrentRound(roundNumber);
            dispatch.setNextAttemptAt(expiresAt);
            orderDispatchRepository.save(dispatch);
        });

        eventPublisher.publishEvent(new DeliveryOfferCreatedEvent(
                offer.getId(),
                order.getId(),
                agent.getId(),
                roundNumber,
                radiusKm,
                expiresAt,
                now
        ));

        return offer;
    }

    @Override
    @Transactional
    public void recordRetryAttempt(UUID orderId, int currentRound, Instant nextAttemptAt) {
        orderDispatchRepository.findByOrderId(orderId).ifPresent(dispatch -> {
            dispatch.setCurrentRound(currentRound);
            dispatch.setNextAttemptAt(nextAttemptAt);
            orderDispatchRepository.save(dispatch);
        });
    }

    @Override
    @Transactional
    public boolean markDispatchExhausted(UUID orderId) {
        Optional<OrderDispatch> dispatchOpt = orderDispatchRepository.findByOrderId(orderId);
        if (dispatchOpt.isEmpty() || dispatchOpt.get().getStatus() == DeliveryDispatchStatus.EXHAUSTED) {
            return false;
        }

        OrderDispatch dispatch = dispatchOpt.get();
        dispatch.setStatus(DeliveryDispatchStatus.EXHAUSTED);
        orderDispatchRepository.save(dispatch);

        deliveryOfferRepository.findByOrderIdAndStatus(orderId, DeliveryOfferStatus.PENDING)
                .ifPresent(o -> {
                    o.setStatus(DeliveryOfferStatus.WITHDRAWN);
                    o.setRespondedAt(Instant.now());
                    deliveryOfferRepository.save(o);
                });

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || orderLifecycleService.isTerminal(order.getCurrentStatus())) {
            return false;
        }

        UUID restaurantId = order.getRestaurant() != null ? order.getRestaurant().getId() : null;
        UUID customerId = order.getCustomer() != null ? order.getCustomer().getId() : null;

        eventPublisher.publishEvent(new DeliveryDispatchExhaustedEvent(
                orderId,
                restaurantId,
                customerId,
                Instant.now()
        ));

        return true;
    }

    @Override
    @Transactional(noRollbackFor = OfferExpiredException.class)
    public void assignAgent(UUID orderId, UUID agentUserId) {
        DeliveryAgent unlockedAgent = loadDeliveryAgentForUser(agentUserId);
        DeliveryAgent agent = deliveryAgentRepository.findByIdForUpdate(unlockedAgent.getId())
                .orElse(unlockedAgent);

        if (!agent.isAvailable() || agent.isAssigned()) {
            throw new BadRequestException("You are not available or are already assigned to an active delivery.");
        }

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (order.getDeliveryAgent() != null || orderLifecycleService.isTerminal(order.getCurrentStatus())) {
            throw new ResourceConflictException("This order has already been assigned or is no longer active.");
        }

        OrderDispatch dispatch = orderDispatchRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch not found for order: " + orderId));

        if (dispatch.getStatus() != DeliveryDispatchStatus.FINDING_AGENT) {
            throw new ResourceConflictException("Dispatch is no longer seeking an agent for this order.");
        }

        DeliveryOffer offer = deliveryOfferRepository.findByOrderIdAndAgentIdAndStatus(
                orderId, agent.getId(), DeliveryOfferStatus.PENDING)
                .orElseThrow(() -> new ResourceConflictException("No active offer found for your account on this order."));

        if (Instant.now().isAfter(offer.getExpiresAt())) {
            offer.setStatus(DeliveryOfferStatus.EXPIRED);
            offer.setRespondedAt(Instant.now());
            deliveryOfferRepository.save(offer);
            throw new OfferExpiredException("The offer window has expired.");
        }

        // Win offer
        offer.setStatus(DeliveryOfferStatus.ACCEPTED);
        offer.setRespondedAt(Instant.now());
        deliveryOfferRepository.save(offer);

        order.setDeliveryAgent(agent);
        orderRepository.save(order);

        dispatch.setStatus(DeliveryDispatchStatus.AGENT_ASSIGNED);
        orderDispatchRepository.save(dispatch);

        agent.setAssigned(true);
        agent.setLastAssignedAt(Instant.now());
        deliveryAgentRepository.save(agent);

        eventPublisher.publishEvent(new DeliveryAgentAssignedEvent(
                order.getId(),
                order.getCustomer().getId(),
                agent.getId(),
                agent.getUser().getName(),
                agent.getUser().getPhoneNumber(),
                Instant.now()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> getOfferedAgentIds(UUID orderId) {
        return deliveryOfferRepository.findOfferedAgentIdsByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderDispatch> findDispatch(UUID orderId) {
        return orderDispatchRepository.findByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDispatchActive(UUID orderId) {
        return orderDispatchRepository.findByOrderId(orderId)
                .map(d -> d.getStatus() == DeliveryDispatchStatus.FINDING_AGENT)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAgent findAgentForUser(UUID userId) {
        return loadDeliveryAgentForUser(userId);
    }

    private DeliveryAgent loadDeliveryAgentForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return deliveryAgentRepository.findByUser(user)
                .orElseThrow(() -> new DeliveryAgentNotFoundException("Delivery agent profile not found for user: " + userId));
    }
}
