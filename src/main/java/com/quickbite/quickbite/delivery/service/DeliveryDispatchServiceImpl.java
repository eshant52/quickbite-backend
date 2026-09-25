package com.quickbite.quickbite.delivery.service;

import com.quickbite.quickbite.common.config.property.DispatchProperties;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceConflictException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.delivery.dto.OrderOfferSummaryResponse;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.model.DeliveryDispatchStatus;
import com.quickbite.quickbite.delivery.model.DeliveryOffer;
import com.quickbite.quickbite.delivery.model.DeliveryOfferStatus;
import com.quickbite.quickbite.delivery.model.OrderDispatch;
import com.quickbite.quickbite.delivery.repository.DeliveryOfferRepository;
import com.quickbite.quickbite.delivery.service.strategy.DeliveryCandidateSelector;
import com.quickbite.quickbite.delivery.service.strategy.DeliveryEarningsCalculator;
import com.quickbite.quickbite.order.exception.OrderNotFoundException;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.order.service.OrderLifecycleService;
import com.quickbite.quickbite.user.model.Address;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Orchestrates the delivery dispatch workflow:
 * <ul>
 *     <li>Acquires distributed locks to ensure single-pod execution per order</li>
 *     <li>Evaluates spatial candidates and road travel times outside of database transactions</li>
 *     <li>Delegates atomic state transitions to {@link DeliveryDispatchLifecycleService}</li>
 *     <li>Enforces time-windowed escalation rounds and polling intervals</li>
 * </ul>
 */
@Service
public class DeliveryDispatchServiceImpl implements DeliveryDispatchService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryDispatchServiceImpl.class);
    private static final String DISPATCH_LOCK_PREFIX = "quickbite:dispatch-lock:";
    private static final long LOCK_WAIT_SECONDS = 0;
    private static final long LOCK_LEASE_SECONDS = 15;

    private final DeliveryDispatchLifecycleService lifecycleService;
    private final DeliveryCandidateSelector candidateSelector;
    private final DeliveryEarningsCalculator earningsCalculator;
    private final OrderLifecycleService orderLifecycleService;
    private final DispatchProperties dispatchProperties;
    private final RedissonClient redissonClient;
    private final OrderRepository orderRepository;
    private final DeliveryOfferRepository deliveryOfferRepository;

    public DeliveryDispatchServiceImpl(
            DeliveryDispatchLifecycleService lifecycleService,
            DeliveryCandidateSelector candidateSelector,
            DeliveryEarningsCalculator earningsCalculator,
            OrderLifecycleService orderLifecycleService,
            DispatchProperties dispatchProperties,
            RedissonClient redissonClient,
            OrderRepository orderRepository,
            DeliveryOfferRepository deliveryOfferRepository) {
        this.lifecycleService = lifecycleService;
        this.candidateSelector = candidateSelector;
        this.earningsCalculator = earningsCalculator;
        this.orderLifecycleService = orderLifecycleService;
        this.dispatchProperties = dispatchProperties;
        this.redissonClient = redissonClient;
        this.orderRepository = orderRepository;
        this.deliveryOfferRepository = deliveryOfferRepository;
    }

    @Override
    public void initiateDispatch(UUID orderId) {
        RLock lock = redissonClient.getLock(DISPATCH_LOCK_PREFIX + orderId);
        boolean locked;
        try {
            locked = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Request interrupted. Please try again.");
        }

        if (!locked) {
            log.info("Another dispatch action is in progress for order {}, skipping initiate", orderId);
            return;
        }

        try {
            Duration totalDispatchWindow = dispatchProperties.rounds().stream()
                    .map(DispatchProperties.DispatchRoundProperties::roundDuration)
                    .reduce(Duration.ZERO, Duration::plus);

            Optional<OrderDispatch> dispatchOpt = lifecycleService.createInitialDispatch(orderId, totalDispatchWindow);
            if (dispatchOpt.isEmpty()) {
                log.info("Dispatch already initiated for order {}, skipping", orderId);
                return;
            }

            OrderDispatch dispatch = dispatchOpt.get();
            Optional<Order> opOrder = orderLifecycleService.getOrderIfNotInTerminalState(orderId);
            if (opOrder.isEmpty()) {
                exhaustDispatch(orderId);
                return;
            }
            Order order = opOrder.get();

            // Candidate evaluation (including RoutingGateway HTTP call) happens OUTSIDE any DB transaction:
            advanceToNextCandidateOrRound(order, dispatch, 0);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public void rejectOffer(UUID orderId, UUID agentUserId) {
        RLock lock = redissonClient.getLock(DISPATCH_LOCK_PREFIX + orderId);
        boolean locked;
        try {
            locked = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Request interrupted. Please try again.");
        }

        if (!locked) {
            throw new ResourceConflictException("Another dispatch action is in progress for this order.");
        }

        try {
            processAgentRejection(orderId, agentUserId);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    protected void processAgentRejection(UUID orderId, UUID agentUserId) {
        Optional<OrderDispatch> dispatchOpt = lifecycleService.recordOfferRejection(orderId, agentUserId);
        if (dispatchOpt.isEmpty()) {
            log.warn("No pending offer found to reject for agent user {} and order {}", agentUserId, orderId);
            return;
        }

        OrderDispatch dispatch = dispatchOpt.get();
        if (dispatch.getStatus() != DeliveryDispatchStatus.FINDING_AGENT) {
            return;
        }

        Optional<Order> opOrder = orderLifecycleService.getOrderIfNotInTerminalState(orderId);
        if (opOrder.isEmpty()) {
            exhaustDispatch(orderId);
            return;
        }
        Order order = opOrder.get();

        // Immediately attempt next candidate in the same round (outside DB transaction)
        advanceToNextCandidateOrRound(order, dispatch, dispatch.getCurrentRound());
    }

    @Override
    public void processDueDispatch(UUID orderId) {
        RLock lock = redissonClient.getLock(DISPATCH_LOCK_PREFIX + orderId);
        boolean locked;
        try {
            locked = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        if (!locked) {
            return; // Another pod or thread is handling this dispatch
        }

        try {
            executeDueDispatch(orderId);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    protected void executeDueDispatch(UUID orderId) {
        if (!lifecycleService.isDispatchActive(orderId)) {
            return;
        }
        OrderDispatch dispatch = lifecycleService.findDispatch(orderId).orElse(null);
        if (dispatch == null || dispatch.getStatus() != DeliveryDispatchStatus.FINDING_AGENT) {
            return;
        }

        Instant now = Instant.now();

        // 1. Check pending offer in short transaction
        PendingOfferCheckResult offerCheck = lifecycleService.checkAndExpirePendingOffer(orderId);
        if (offerCheck.isActive()) {
            // Current offer still within its window - allow the driver to decide
            if (dispatch.getNextAttemptAt() == null || dispatch.getNextAttemptAt().isBefore(offerCheck.expiresAt())) {
                lifecycleService.recordRetryAttempt(orderId, dispatch.getCurrentRound(), offerCheck.expiresAt());
            }
            return;
        }

        // 2. Check if overall 10-minute dispatch deadline expired
        if (dispatch.getDispatchDeadline() != null && !now.isBefore(dispatch.getDispatchDeadline())) {
            exhaustDispatch(orderId);
            return;
        }

        Optional<Order> opOrder = orderLifecycleService.getOrderIfNotInTerminalState(orderId);
        if (opOrder.isEmpty()) {
            exhaustDispatch(orderId);
            return;
        }
        Order order = opOrder.get();

        // 3. Candidate evaluation (including RoutingGateway HTTP call) happens OUTSIDE any DB transaction
        advanceToNextCandidateOrRound(order, dispatch, dispatch.getCurrentRound());
    }

    @Override
    public void acceptOffer(UUID orderId, UUID agentUserId) {
        RLock lock = redissonClient.getLock(DISPATCH_LOCK_PREFIX + orderId);
        boolean locked;
        try {
            locked = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Request interrupted. Please try again.");
        }

        if (!locked) {
            throw new ResourceConflictException("Another acceptance request is in progress for this order.");
        }

        try {
            lifecycleService.assignAgent(orderId, agentUserId);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderOfferSummaryResponse getOfferSummary(UUID orderId, UUID agentUserId) {
        DeliveryAgent agent = lifecycleService.findAgentForUser(agentUserId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        DeliveryOffer offer = deliveryOfferRepository.findByOrderIdAndAgentIdAndStatusIn(
                        orderId,
                        agent.getId(),
                        List.of(DeliveryOfferStatus.PENDING, DeliveryOfferStatus.ACCEPTED)
                )
                .orElseThrow(() -> new ResourceNotFoundException("No active offer found for order " + orderId));

        DeliveryEarningsCalculator.DeliveryEarningsBreakdown earnings =
                earningsCalculator.calculateEarnings(order, agent);

        long secondsRemaining = Math.max(0, Duration.between(Instant.now(), offer.getExpiresAt()).toSeconds());

        String restaurantAddress = (order.getRestaurant() != null && order.getRestaurant().getAddress() != null)
                ? formatAddress(order.getRestaurant().getAddress())
                : "";

        return new OrderOfferSummaryResponse(
                order.getId(),
                order.getRestaurant() != null ? order.getRestaurant().getName() : "",
                restaurantAddress,
                order.getDeliveryAddress(),
                earnings.deliveryDistanceKm(),
                earnings.estimatedDeliverySeconds(),
                earnings.guaranteedPayout(),
                earnings.tipAmount(),
                earnings.estimatedTotalPayout(),
                offer.getExpiresAt(),
                secondsRemaining
        );
    }

    // ── Internal Dispatch Helper ──────────────────────────────────────────────

    private void advanceToNextCandidateOrRound(Order order, OrderDispatch fallbackDispatch, int startRoundIndex) {
        OrderDispatch dispatch = lifecycleService.findDispatch(order.getId()).orElse(fallbackDispatch);
        if (dispatch == null || dispatch.getStatus() != DeliveryDispatchStatus.FINDING_AGENT) {
            return;
        }

        Instant now = Instant.now();
        if (dispatch.getDispatchDeadline() != null && !now.isBefore(dispatch.getDispatchDeadline())) {
            exhaustDispatch(order.getId());
            return;
        }

        Set<UUID> excludedAgentIds = lifecycleService.getOfferedAgentIds(order.getId());
        int totalRounds = dispatchProperties.rounds().size();
        int effectiveStartRound = Math.max(startRoundIndex, dispatch.getCurrentRound());

        for (int r = effectiveStartRound; r < totalRounds; r++) {
            Instant roundDeadline = calculateRoundDeadline(dispatch.getStartedAt(), r);
            if (!now.isBefore(roundDeadline)) {
                continue;
            }

            DispatchProperties.DispatchRoundProperties roundConfig = dispatchProperties.rounds().get(r);

            // OUTSIDE DB TRANSACTION:
            // candidateSelector may call RoutingGateway.travelTimes (HTTP REST call)
            Optional<DeliveryAgent> candidate = candidateSelector.selectNextCandidate(
                    order, roundConfig.radiusKm(), excludedAgentIds);

            if (candidate.isPresent()) {
                DeliveryAgent agent = candidate.get();
                Instant expiresAt = now.plus(dispatchProperties.driverOfferTimeout());

                // ATOMIC WRITE IN SHORT TRANSACTION:
                lifecycleService.recordCreatedOffer(
                        order.getId(),
                        agent,
                        r,
                        roundConfig.radiusKm(),
                        expiresAt
                );
                return;
            }

            // No candidate found in this active round:
            // Schedule retry in this round after retryInterval, capped by roundDeadline
            Instant nextAttempt = now.plus(dispatchProperties.retryInterval());
            if (nextAttempt.isAfter(roundDeadline)) {
                nextAttempt = roundDeadline;
            }

            // ATOMIC WRITE IN SHORT TRANSACTION:
            lifecycleService.recordRetryAttempt(order.getId(), r, nextAttempt);
            return;
        }

        // All rounds and candidates exhausted:
        exhaustDispatch(order.getId());
    }

    private void exhaustDispatch(UUID orderId) {
        boolean shouldCancelOrder = lifecycleService.markDispatchExhausted(orderId);
        if (shouldCancelOrder) {
            orderLifecycleService.cancelDueToNoDeliveryAgent(orderId);
        }
    }

    private Instant calculateRoundDeadline(Instant startedAt, int roundIndex) {
        if (startedAt == null) {
            return Instant.now();
        }
        List<DispatchProperties.DispatchRoundProperties> rounds = dispatchProperties.rounds();
        if (rounds.isEmpty()) {
            return startedAt;
        }
        if (roundIndex < 0 || roundIndex >= rounds.size()) {
            throw new IllegalArgumentException(
                    "Invalid dispatch round index: " + roundIndex + " (configured rounds: " + rounds.size() + ")");
        }
        Duration cumulative = Duration.ZERO;
        for (int i = 0; i <= roundIndex; i++) {
            cumulative = cumulative.plus(rounds.get(i).roundDuration());
        }
        return startedAt.plus(cumulative);
    }

    private String formatAddress(Address addr) {
        if (addr == null) {
            return "";
        }
        return Stream.of(addr.getHouseNumber(), addr.getBuildingName(), addr.getStreet(), addr.getCity())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
    }
}
