package com.quickbite.quickbite.delivery.service;

import com.quickbite.quickbite.common.config.property.DispatchProperties;
import com.quickbite.quickbite.common.exception.ResourceConflictException;
import com.quickbite.quickbite.delivery.dto.OrderOfferSummaryResponse;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.model.DeliveryDispatchStatus;
import com.quickbite.quickbite.delivery.model.DeliveryOffer;
import com.quickbite.quickbite.delivery.model.DeliveryOfferStatus;
import com.quickbite.quickbite.delivery.model.OrderDispatch;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.delivery.repository.DeliveryOfferRepository;
import com.quickbite.quickbite.delivery.service.strategy.DeliveryCandidateSelector;
import com.quickbite.quickbite.delivery.service.strategy.DeliveryEarningsCalculator;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.order.service.OrderLifecycleService;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryDispatchServiceImplTest {

    @Mock private DeliveryDispatchLifecycleService lifecycleService;
    @Mock private DeliveryCandidateSelector candidateSelector;
    @Mock private DeliveryEarningsCalculator earningsCalculator;
    @Mock private OrderLifecycleService orderLifecycleService;
    @Mock private RedissonClient redissonClient;
    @Mock private RLock rLock;
    @Mock private OrderRepository orderRepository;
    @Mock private DeliveryOfferRepository deliveryOfferRepository;

    private DispatchProperties dispatchProperties;
    private DeliveryDispatchServiceImpl dispatchService;

    private UUID orderId;
    private UUID agentUserId;
    private Order order;
    private User agentUser;
    private DeliveryAgent agent;

    @BeforeEach
    void setUp() throws InterruptedException {
        dispatchProperties = new DispatchProperties(
                List.of(
                        new DispatchProperties.DispatchRoundProperties(3.0, Duration.ofMinutes(2)),
                        new DispatchProperties.DispatchRoundProperties(6.0, Duration.ofMinutes(3)),
                        new DispatchProperties.DispatchRoundProperties(10.0, Duration.ofMinutes(5))
                ),
                Duration.ofSeconds(30),
                Duration.ofSeconds(10),
                Duration.ofSeconds(45),
                10,
                new DispatchProperties.EarningsProperties(new BigDecimal("30.00"), new BigDecimal("10.00"))
        );

        dispatchService = new DeliveryDispatchServiceImpl(
                lifecycleService,
                candidateSelector,
                earningsCalculator,
                orderLifecycleService,
                dispatchProperties,
                redissonClient,
                orderRepository,
                deliveryOfferRepository
        );

        orderId = UUID.randomUUID();
        agentUserId = UUID.randomUUID();

        User customer = new User();
        customer.setId(UUID.randomUUID());
        customer.setName("Customer One");

        Restaurant restaurant = new Restaurant();
        restaurant.setId(UUID.randomUUID());
        restaurant.setName("Bite Palace");
        Address restAddr = new Address();
        restAddr.setStreet("123 Food Street");
        restaurant.setAddress(restAddr);

        order = new Order();
        order.setId(orderId);
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setCurrentStatus(OrderStatus.ACCEPTED);
        order.setDeliveryAddress("456 Hungry Lane");

        agentUser = new User();
        agentUser.setId(agentUserId);
        agentUser.setName("Agent Bob");
        agentUser.setPhoneNumber("9876543210");

        agent = new DeliveryAgent();
        agent.setId(UUID.randomUUID());
        agent.setUser(agentUser);
        agent.setAvailable(true);
        agent.setAssigned(false);

        lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
        lenient().when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        lenient().when(rLock.isHeldByCurrentThread()).thenReturn(true);
        lenient().when(lifecycleService.isDispatchActive(orderId)).thenReturn(true);
    }

    @Test
    @DisplayName("initiateDispatch creates OrderDispatch, selects candidate, and delegates atomic offer creation")
    void initiateDispatch_candidateFound_createsPendingOffer() {
        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setOrderId(orderId);
        dispatch.setStatus(DeliveryDispatchStatus.FINDING_AGENT);
        dispatch.setStartedAt(Instant.now());
        dispatch.setCurrentRound(0);
        dispatch.setDispatchDeadline(Instant.now().plus(Duration.ofMinutes(10)));

        when(lifecycleService.createInitialDispatch(eq(orderId), any(Duration.class))).thenReturn(Optional.of(dispatch));
        when(lifecycleService.findDispatch(orderId)).thenReturn(Optional.of(dispatch));
        when(orderLifecycleService.getOrderIfNotInTerminalState(orderId)).thenReturn(Optional.of(order));
        when(lifecycleService.getOfferedAgentIds(orderId)).thenReturn(Set.of());
        when(candidateSelector.selectNextCandidate(eq(order), eq(3.0), anySet()))
                .thenReturn(Optional.of(agent));

        dispatchService.initiateDispatch(orderId);

        verify(lifecycleService).recordCreatedOffer(
                eq(orderId),
                eq(agent),
                eq(0),
                eq(3.0),
                any(Instant.class)
        );
    }

    @Test
    @DisplayName("initiateDispatch stays in Round 0 and schedules retry if Round 0 has no candidates")
    void initiateDispatch_noRound0Candidates_staysInRound0AndSchedulesRetry() {
        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setOrderId(orderId);
        dispatch.setStatus(DeliveryDispatchStatus.FINDING_AGENT);
        dispatch.setStartedAt(Instant.now());
        dispatch.setCurrentRound(0);
        dispatch.setDispatchDeadline(Instant.now().plus(Duration.ofMinutes(10)));

        when(lifecycleService.createInitialDispatch(eq(orderId), any(Duration.class))).thenReturn(Optional.of(dispatch));
        when(lifecycleService.findDispatch(orderId)).thenReturn(Optional.of(dispatch));
        when(orderLifecycleService.getOrderIfNotInTerminalState(orderId)).thenReturn(Optional.of(order));
        when(lifecycleService.getOfferedAgentIds(orderId)).thenReturn(Set.of());
        when(candidateSelector.selectNextCandidate(eq(order), eq(3.0), anySet()))
                .thenReturn(Optional.empty());

        dispatchService.initiateDispatch(orderId);

        verify(lifecycleService).recordRetryAttempt(eq(orderId), eq(0), any(Instant.class));
        verify(candidateSelector, never()).selectNextCandidate(eq(order), eq(6.0), anySet());
        verify(lifecycleService, never()).recordCreatedOffer(any(), any(), anyInt(), anyDouble(), any());
    }

    @Test
    @DisplayName("processDueDispatch escalates to Round 1 after Round 0's 2-minute window elapses")
    void processDueDispatch_round0WindowElapsed_escalatesToRound1() {
        Instant startedAt = Instant.now().minus(Duration.ofMinutes(2).plusSeconds(1));
        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setOrderId(orderId);
        dispatch.setStatus(DeliveryDispatchStatus.FINDING_AGENT);
        dispatch.setStartedAt(startedAt);
        dispatch.setCurrentRound(0);
        dispatch.setDispatchDeadline(startedAt.plus(Duration.ofMinutes(10)));
        dispatch.setNextAttemptAt(Instant.now().minusSeconds(1));

        when(lifecycleService.findDispatch(orderId)).thenReturn(Optional.of(dispatch));
        when(orderLifecycleService.getOrderIfNotInTerminalState(orderId)).thenReturn(Optional.of(order));
        when(lifecycleService.checkAndExpirePendingOffer(orderId)).thenReturn(PendingOfferCheckResult.noPendingOffer());
        when(lifecycleService.getOfferedAgentIds(orderId)).thenReturn(Set.of());
        when(candidateSelector.selectNextCandidate(eq(order), eq(6.0), anySet()))
                .thenReturn(Optional.of(agent));

        dispatchService.processDueDispatch(orderId);

        verify(lifecycleService).recordCreatedOffer(
                eq(orderId),
                eq(agent),
                eq(1),
                eq(6.0),
                any(Instant.class)
        );
    }

    @Test
    @DisplayName("rejectOffer immediately offers to next candidate in the same round")
    void rejectOffer_candidateAvailableInSameRound_offersToNextCandidate() {
        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setOrderId(orderId);
        dispatch.setStatus(DeliveryDispatchStatus.FINDING_AGENT);
        dispatch.setStartedAt(Instant.now());
        dispatch.setCurrentRound(0);
        dispatch.setDispatchDeadline(Instant.now().plus(Duration.ofMinutes(10)));

        DeliveryAgent nextAgent = new DeliveryAgent();
        nextAgent.setId(UUID.randomUUID());

        when(lifecycleService.recordOfferRejection(orderId, agentUserId)).thenReturn(Optional.of(dispatch));
        when(lifecycleService.findDispatch(orderId)).thenReturn(Optional.of(dispatch));
        when(orderLifecycleService.getOrderIfNotInTerminalState(orderId)).thenReturn(Optional.of(order));
        when(lifecycleService.getOfferedAgentIds(orderId)).thenReturn(Set.of(agent.getId()));
        when(candidateSelector.selectNextCandidate(eq(order), eq(3.0), anySet()))
                .thenReturn(Optional.of(nextAgent));

        dispatchService.rejectOffer(orderId, agentUserId);

        verify(lifecycleService).recordCreatedOffer(
                eq(orderId),
                eq(nextAgent),
                eq(0),
                eq(3.0),
                any(Instant.class)
        );
    }

    @Test
    @DisplayName("rejectOffer with no immediate candidate stays in current round and schedules retry")
    void rejectOffer_noOtherCandidateInCurrentRound_schedulesRetryInCurrentRound() {
        Instant startedAt = Instant.now().minusSeconds(30);
        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setOrderId(orderId);
        dispatch.setStatus(DeliveryDispatchStatus.FINDING_AGENT);
        dispatch.setStartedAt(startedAt);
        dispatch.setCurrentRound(0);
        dispatch.setDispatchDeadline(startedAt.plus(Duration.ofMinutes(10)));

        when(lifecycleService.recordOfferRejection(orderId, agentUserId)).thenReturn(Optional.of(dispatch));
        when(lifecycleService.findDispatch(orderId)).thenReturn(Optional.of(dispatch));
        when(orderLifecycleService.getOrderIfNotInTerminalState(orderId)).thenReturn(Optional.of(order));
        when(lifecycleService.getOfferedAgentIds(orderId)).thenReturn(Set.of(agent.getId()));
        when(candidateSelector.selectNextCandidate(eq(order), eq(3.0), anySet()))
                .thenReturn(Optional.empty());

        dispatchService.rejectOffer(orderId, agentUserId);

        verify(lifecycleService).recordRetryAttempt(eq(orderId), eq(0), any(Instant.class));
        verify(candidateSelector, never()).selectNextCandidate(eq(order), eq(6.0), anySet());
    }

    @Test
    @DisplayName("rejectOffer when round deadline has passed escalates to next round")
    void rejectOffer_roundDeadlinePassed_escalatesToNextRound() {
        // Offer was sent at 1m45s, rejected at 2m10s (past round 0's 2-minute deadline)
        Instant startedAt = Instant.now().minus(Duration.ofMinutes(2).plusSeconds(10));
        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setOrderId(orderId);
        dispatch.setStatus(DeliveryDispatchStatus.FINDING_AGENT);
        dispatch.setStartedAt(startedAt);
        dispatch.setCurrentRound(0);
        dispatch.setDispatchDeadline(startedAt.plus(Duration.ofMinutes(10)));

        DeliveryAgent nextAgent = new DeliveryAgent();
        nextAgent.setId(UUID.randomUUID());

        when(lifecycleService.recordOfferRejection(orderId, agentUserId)).thenReturn(Optional.of(dispatch));
        when(lifecycleService.findDispatch(orderId)).thenReturn(Optional.of(dispatch));
        when(orderLifecycleService.getOrderIfNotInTerminalState(orderId)).thenReturn(Optional.of(order));
        when(lifecycleService.getOfferedAgentIds(orderId)).thenReturn(Set.of(agent.getId()));
        when(candidateSelector.selectNextCandidate(eq(order), eq(6.0), anySet()))
                .thenReturn(Optional.of(nextAgent));

        dispatchService.rejectOffer(orderId, agentUserId);

        verify(lifecycleService).recordCreatedOffer(
                eq(orderId),
                eq(nextAgent),
                eq(1),
                eq(6.0),
                any(Instant.class)
        );
    }

    @Test
    @DisplayName("acceptOffer delegates to lifecycleService to assign agent atomically")
    void acceptOffer_success() {
        dispatchService.acceptOffer(orderId, agentUserId);

        verify(lifecycleService).assignAgent(orderId, agentUserId);
    }

    @Test
    @DisplayName("acceptOffer propagates OfferExpiredException from lifecycleService")
    void acceptOffer_expiredOffer_throwsOfferExpiredException() {
        doThrow(new com.quickbite.quickbite.delivery.exception.OfferExpiredException("Offer has expired"))
                .when(lifecycleService).assignAgent(orderId, agentUserId);

        assertThatThrownBy(() -> dispatchService.acceptOffer(orderId, agentUserId))
                .isInstanceOf(com.quickbite.quickbite.delivery.exception.OfferExpiredException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("acceptOffer throws ResourceConflictException when lock cannot be acquired")
    void acceptOffer_lockFailed_throwsConflict() throws InterruptedException {
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        assertThatThrownBy(() -> dispatchService.acceptOffer(orderId, agentUserId))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("progress");

        verify(lifecycleService, never()).assignAgent(any(), any());
    }

    @Test
    @DisplayName("processDueDispatch exhausts dispatch when deadline has expired")
    void processDueDispatch_deadlineExpired_exhaustsAndCancels() {
        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setOrderId(orderId);
        dispatch.setStatus(DeliveryDispatchStatus.FINDING_AGENT);
        dispatch.setDispatchDeadline(Instant.now().minusSeconds(1)); // deadline passed

        when(lifecycleService.findDispatch(orderId)).thenReturn(Optional.of(dispatch));
        when(lifecycleService.checkAndExpirePendingOffer(orderId)).thenReturn(PendingOfferCheckResult.noPendingOffer());
        when(lifecycleService.markDispatchExhausted(orderId)).thenReturn(true);

        dispatchService.processDueDispatch(orderId);

        verify(lifecycleService).markDispatchExhausted(orderId);
        verify(orderLifecycleService).cancelDueToNoDeliveryAgent(orderId);
    }

    @Test
    @DisplayName("processDueDispatch does not cancel order if markDispatchExhausted returns false")
    void processDueDispatch_deadlineExpiredButMarkExhaustedFalse_skipsOrderCancel() {
        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setOrderId(orderId);
        dispatch.setStatus(DeliveryDispatchStatus.FINDING_AGENT);
        dispatch.setDispatchDeadline(Instant.now().minusSeconds(1));

        when(lifecycleService.findDispatch(orderId)).thenReturn(Optional.of(dispatch));
        when(lifecycleService.checkAndExpirePendingOffer(orderId)).thenReturn(PendingOfferCheckResult.noPendingOffer());
        when(lifecycleService.markDispatchExhausted(orderId)).thenReturn(false);

        dispatchService.processDueDispatch(orderId);

        verify(lifecycleService).markDispatchExhausted(orderId);
        verify(orderLifecycleService, never()).cancelDueToNoDeliveryAgent(any());
    }

    @Test
    @DisplayName("getOfferSummary returns correct earning breakdown and route details")
    void getOfferSummary_success() {
        DeliveryOffer offer = new DeliveryOffer();
        offer.setId(UUID.randomUUID());
        offer.setOrderId(orderId);
        offer.setAgent(agent);
        offer.setStatus(DeliveryOfferStatus.PENDING);
        offer.setExpiresAt(Instant.now().plusSeconds(120));

        when(lifecycleService.findAgentForUser(agentUserId)).thenReturn(agent);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(deliveryOfferRepository.findByOrderIdAndAgentIdAndStatusIn(
                orderId,
                agent.getId(),
                List.of(DeliveryOfferStatus.PENDING, DeliveryOfferStatus.ACCEPTED)
        )).thenReturn(Optional.of(offer));

        DeliveryEarningsCalculator.DeliveryEarningsBreakdown breakdown =
                new DeliveryEarningsCalculator.DeliveryEarningsBreakdown(
                        new BigDecimal("45.00"),
                        new BigDecimal("20.00"),
                        new BigDecimal("65.00"),
                        3.5,
                        600L
                );
        when(earningsCalculator.calculateEarnings(order, agent)).thenReturn(breakdown);

        OrderOfferSummaryResponse summary = dispatchService.getOfferSummary(orderId, agentUserId);

        assertThat(summary).isNotNull();
        assertThat(summary.restaurantName()).isEqualTo("Bite Palace");
        assertThat(summary.guaranteedPayout()).isEqualByComparingTo("45.00");
        assertThat(summary.tipAmount()).isEqualByComparingTo("20.00");
        assertThat(summary.estimatedTotalPayout()).isEqualByComparingTo("65.00");
        assertThat(summary.secondsRemaining()).isGreaterThan(0);
    }

    @Test
    @DisplayName("processDueDispatch does not expire offer if still within offer window")
    void processDueDispatch_activeOfferPending_doesNotExpire() {
        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setOrderId(orderId);
        dispatch.setStatus(DeliveryDispatchStatus.FINDING_AGENT);
        dispatch.setStartedAt(Instant.now().minusSeconds(10));
        dispatch.setDispatchDeadline(Instant.now().plus(Duration.ofMinutes(9)));

        when(lifecycleService.findDispatch(orderId)).thenReturn(Optional.of(dispatch));
        when(lifecycleService.checkAndExpirePendingOffer(orderId))
                .thenReturn(PendingOfferCheckResult.active(Instant.now().plusSeconds(35)));

        dispatchService.processDueDispatch(orderId);

        verify(candidateSelector, never()).selectNextCandidate(any(), anyDouble(), anySet());
        verify(lifecycleService, never()).recordCreatedOffer(any(), any(), anyInt(), anyDouble(), any());
    }

    @Test
    @DisplayName("processDueDispatch exhausts when all round windows have elapsed and no candidate found")
    void processDueDispatch_allRoundsExpired_exhaustsDispatch() {
        Instant startedAt = Instant.now().minus(Duration.ofMinutes(10).plusSeconds(1));
        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setOrderId(orderId);
        dispatch.setStatus(DeliveryDispatchStatus.FINDING_AGENT);
        dispatch.setStartedAt(startedAt);
        dispatch.setCurrentRound(2);
        dispatch.setDispatchDeadline(startedAt.plus(Duration.ofMinutes(15)));

        when(lifecycleService.findDispatch(orderId)).thenReturn(Optional.of(dispatch));
        when(orderLifecycleService.getOrderIfNotInTerminalState(orderId)).thenReturn(Optional.of(order));
        when(lifecycleService.checkAndExpirePendingOffer(orderId)).thenReturn(PendingOfferCheckResult.noPendingOffer());
        when(lifecycleService.markDispatchExhausted(orderId)).thenReturn(true);

        dispatchService.processDueDispatch(orderId);

        verify(lifecycleService).markDispatchExhausted(orderId);
        verify(orderLifecycleService).cancelDueToNoDeliveryAgent(orderId);
    }

    @Test
    @DisplayName("processDueDispatch honors active pending offer even if overall dispatch deadline has passed")
    void processDueDispatch_activeOfferPendingPastDispatchDeadline_doesNotCancelPrematurely() {
        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setOrderId(orderId);
        dispatch.setStatus(DeliveryDispatchStatus.FINDING_AGENT);
        dispatch.setStartedAt(Instant.now().minus(Duration.ofMinutes(10)));
        // Dispatch deadline passed 5 seconds ago:
        dispatch.setDispatchDeadline(Instant.now().minusSeconds(5));

        when(lifecycleService.findDispatch(orderId)).thenReturn(Optional.of(dispatch));
        // Driver still has 25 seconds remaining to decide:
        when(lifecycleService.checkAndExpirePendingOffer(orderId))
                .thenReturn(PendingOfferCheckResult.active(Instant.now().plusSeconds(25)));

        dispatchService.processDueDispatch(orderId);

        verify(lifecycleService, never()).markDispatchExhausted(any());
        verify(orderLifecycleService, never()).cancelDueToNoDeliveryAgent(any());
        verify(candidateSelector, never()).selectNextCandidate(any(), anyDouble(), anySet());
    }

    @Test
    @DisplayName("processDueDispatch exhausts when active offer expires after dispatch deadline")
    void processDueDispatch_offerExpiresAfterDispatchDeadline_exhaustsDispatch() {
        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setOrderId(orderId);
        dispatch.setStatus(DeliveryDispatchStatus.FINDING_AGENT);
        dispatch.setStartedAt(Instant.now().minus(Duration.ofMinutes(11)));
        // Dispatch deadline passed 60 seconds ago:
        dispatch.setDispatchDeadline(Instant.now().minusSeconds(60));

        when(lifecycleService.findDispatch(orderId)).thenReturn(Optional.of(dispatch));
        when(lifecycleService.checkAndExpirePendingOffer(orderId))
                .thenReturn(PendingOfferCheckResult.expired());
        when(lifecycleService.markDispatchExhausted(orderId)).thenReturn(true);

        dispatchService.processDueDispatch(orderId);

        verify(lifecycleService).markDispatchExhausted(orderId);
        verify(orderLifecycleService).cancelDueToNoDeliveryAgent(orderId);
    }
}
