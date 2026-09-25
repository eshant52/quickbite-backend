package com.quickbite.quickbite.delivery.service;

import com.quickbite.quickbite.common.event.delivery.DeliveryAgentAssignedEvent;
import com.quickbite.quickbite.common.event.delivery.DeliveryDispatchExhaustedEvent;
import com.quickbite.quickbite.common.event.delivery.DeliveryOfferCreatedEvent;
import com.quickbite.quickbite.common.exception.ResourceConflictException;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.model.DeliveryDispatchStatus;
import com.quickbite.quickbite.delivery.model.DeliveryOffer;
import com.quickbite.quickbite.delivery.model.DeliveryOfferStatus;
import com.quickbite.quickbite.delivery.model.OrderDispatch;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.delivery.repository.DeliveryOfferRepository;
import com.quickbite.quickbite.delivery.repository.OrderDispatchRepository;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.order.service.OrderLifecycleService;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryDispatchLifecycleServiceImplTest {

    @Mock private OrderDispatchRepository orderDispatchRepository;
    @Mock private DeliveryOfferRepository deliveryOfferRepository;
    @Mock private DeliveryAgentRepository deliveryAgentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderLifecycleService orderLifecycleService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private DeliveryDispatchLifecycleServiceImpl lifecycleService;

    private UUID orderId;
    private UUID agentUserId;
    private Order order;
    private User agentUser;
    private DeliveryAgent agent;

    @BeforeEach
    void setUp() {
        lifecycleService = new DeliveryDispatchLifecycleServiceImpl(
                orderDispatchRepository,
                deliveryOfferRepository,
                deliveryAgentRepository,
                orderRepository,
                userRepository,
                orderLifecycleService,
                eventPublisher
        );

        orderId = UUID.randomUUID();
        agentUserId = UUID.randomUUID();

        User customer = new User();
        customer.setId(UUID.randomUUID());
        customer.setName("Customer One");

        Restaurant restaurant = new Restaurant();
        restaurant.setId(UUID.randomUUID());

        order = new Order();
        order.setId(orderId);
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setCurrentStatus(OrderStatus.ACCEPTED);

        agentUser = new User();
        agentUser.setId(agentUserId);
        agentUser.setName("Agent Bob");
        agentUser.setPhoneNumber("9876543210");

        agent = new DeliveryAgent();
        agent.setId(UUID.randomUUID());
        agent.setUser(agentUser);
        agent.setAvailable(true);
        agent.setAssigned(false);
    }

    @Test
    @DisplayName("createInitialDispatch saves OrderDispatch in FINDING_AGENT status")
    void createInitialDispatch_success() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderDispatchRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        Optional<OrderDispatch> result = lifecycleService.createInitialDispatch(orderId, Duration.ofMinutes(10));

        assertThat(result).isPresent();
        OrderDispatch dispatch = result.get();
        assertThat(dispatch.getStatus()).isEqualTo(DeliveryDispatchStatus.FINDING_AGENT);
        assertThat(dispatch.getCurrentRound()).isEqualTo(0);
        assertThat(dispatch.getDispatchDeadline()).isNotNull();

        verify(orderDispatchRepository).save(dispatch);
    }

    @Test
    @DisplayName("createInitialDispatch returns empty when dispatch was already started")
    void createInitialDispatch_alreadyStarted_returnsEmpty() {
        OrderDispatch existing = new OrderDispatch();
        existing.setStatus(DeliveryDispatchStatus.FINDING_AGENT);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderDispatchRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        Optional<OrderDispatch> result = lifecycleService.createInitialDispatch(orderId, Duration.ofMinutes(10));

        assertThat(result).isEmpty();
        verify(orderDispatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("checkAndExpirePendingOffer marks offer EXPIRED when past expiresAt")
    void checkAndExpirePendingOffer_expired_marksExpired() {
        DeliveryOffer offer = new DeliveryOffer();
        offer.setStatus(DeliveryOfferStatus.PENDING);
        offer.setExpiresAt(Instant.now().minusSeconds(5));

        when(deliveryOfferRepository.findByOrderIdAndStatus(orderId, DeliveryOfferStatus.PENDING))
                .thenReturn(Optional.of(offer));

        PendingOfferCheckResult result = lifecycleService.checkAndExpirePendingOffer(orderId);

        assertThat(result.state()).isEqualTo(PendingOfferCheckResult.State.EXPIRED);
        assertThat(offer.getStatus()).isEqualTo(DeliveryOfferStatus.EXPIRED);
        assertThat(offer.getRespondedAt()).isNotNull();
        verify(deliveryOfferRepository).save(offer);
    }

    @Test
    @DisplayName("checkAndExpirePendingOffer returns ACTIVE when still within decision window")
    void checkAndExpirePendingOffer_active_returnsActive() {
        Instant future = Instant.now().plusSeconds(30);
        DeliveryOffer offer = new DeliveryOffer();
        offer.setStatus(DeliveryOfferStatus.PENDING);
        offer.setExpiresAt(future);

        when(deliveryOfferRepository.findByOrderIdAndStatus(orderId, DeliveryOfferStatus.PENDING))
                .thenReturn(Optional.of(offer));

        PendingOfferCheckResult result = lifecycleService.checkAndExpirePendingOffer(orderId);

        assertThat(result.isActive()).isTrue();
        assertThat(result.expiresAt()).isEqualTo(future);
        verify(deliveryOfferRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordOfferRejection marks offer REJECTED and returns updated OrderDispatch")
    void recordOfferRejection_success() {
        DeliveryOffer offer = new DeliveryOffer();
        offer.setStatus(DeliveryOfferStatus.PENDING);

        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setOrderId(orderId);
        dispatch.setStatus(DeliveryDispatchStatus.FINDING_AGENT);

        when(userRepository.findById(agentUserId)).thenReturn(Optional.of(agentUser));
        when(deliveryAgentRepository.findByUser(agentUser)).thenReturn(Optional.of(agent));
        when(deliveryOfferRepository.findByOrderIdAndAgentIdAndStatus(orderId, agent.getId(), DeliveryOfferStatus.PENDING))
                .thenReturn(Optional.of(offer));
        when(orderDispatchRepository.findByOrderId(orderId)).thenReturn(Optional.of(dispatch));

        Optional<OrderDispatch> rejected = lifecycleService.recordOfferRejection(orderId, agentUserId);

        assertThat(rejected).isPresent().contains(dispatch);
        assertThat(offer.getStatus()).isEqualTo(DeliveryOfferStatus.REJECTED);
        assertThat(offer.getRespondedAt()).isNotNull();
        verify(deliveryOfferRepository).save(offer);
    }

    @Test
    @DisplayName("recordCreatedOffer creates PENDING offer, updates dispatch round and timer, and publishes event")
    void recordCreatedOffer_success() {
        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setOrderId(orderId);
        dispatch.setCurrentRound(0);

        when(orderDispatchRepository.findByOrderId(orderId)).thenReturn(Optional.of(dispatch));

        Instant expiresAt = Instant.now().plusSeconds(45);
        DeliveryOffer created = lifecycleService.recordCreatedOffer(orderId, agent, 1, 6.0, expiresAt);

        assertThat(created.getStatus()).isEqualTo(DeliveryOfferStatus.PENDING);
        assertThat(created.getRoundNumber()).isEqualTo(1);
        assertThat(created.getRadiusKm()).isEqualTo(6.0);
        assertThat(created.getExpiresAt()).isEqualTo(expiresAt);

        verify(deliveryOfferRepository).save(created);
        verify(orderDispatchRepository).save(dispatch);
        assertThat(dispatch.getCurrentRound()).isEqualTo(1);
        assertThat(dispatch.getNextAttemptAt()).isEqualTo(expiresAt);

        verify(eventPublisher).publishEvent(any(DeliveryOfferCreatedEvent.class));
    }

    @Test
    @DisplayName("recordRetryAttempt updates dispatch currentRound and nextAttemptAt")
    void recordRetryAttempt_success() {
        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setOrderId(orderId);

        when(orderDispatchRepository.findByOrderId(orderId)).thenReturn(Optional.of(dispatch));

        Instant nextAttempt = Instant.now().plusSeconds(10);
        lifecycleService.recordRetryAttempt(orderId, 0, nextAttempt);

        assertThat(dispatch.getCurrentRound()).isEqualTo(0);
        assertThat(dispatch.getNextAttemptAt()).isEqualTo(nextAttempt);
        verify(orderDispatchRepository).save(dispatch);
    }

    @Test
    @DisplayName("markDispatchExhausted marks dispatch EXHAUSTED, withdraws pending offer, publishes event, and returns true")
    void markDispatchExhausted_success() {
        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setOrderId(orderId);
        dispatch.setStatus(DeliveryDispatchStatus.FINDING_AGENT);

        DeliveryOffer pendingOffer = new DeliveryOffer();
        pendingOffer.setStatus(DeliveryOfferStatus.PENDING);

        when(orderDispatchRepository.findByOrderId(orderId)).thenReturn(Optional.of(dispatch));
        when(deliveryOfferRepository.findByOrderIdAndStatus(orderId, DeliveryOfferStatus.PENDING))
                .thenReturn(Optional.of(pendingOffer));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        boolean shouldCancel = lifecycleService.markDispatchExhausted(orderId);

        assertThat(shouldCancel).isTrue();
        assertThat(dispatch.getStatus()).isEqualTo(DeliveryDispatchStatus.EXHAUSTED);
        assertThat(pendingOffer.getStatus()).isEqualTo(DeliveryOfferStatus.WITHDRAWN);
        verify(deliveryOfferRepository).save(pendingOffer);
        verify(eventPublisher).publishEvent(any(DeliveryDispatchExhaustedEvent.class));
    }

    @Test
    @DisplayName("markDispatchExhausted propagates OptimisticLockException without publishing event (Fix C1)")
    void markDispatchExhausted_versionConflict_doesNotPublishEvent() {
        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setOrderId(orderId);
        dispatch.setStatus(DeliveryDispatchStatus.FINDING_AGENT);

        when(orderDispatchRepository.findByOrderId(orderId)).thenReturn(Optional.of(dispatch));
        doThrow(new jakarta.persistence.OptimisticLockException("Version conflict"))
                .when(orderDispatchRepository).save(dispatch);

        assertThatThrownBy(() -> lifecycleService.markDispatchExhausted(orderId))
                .isInstanceOf(jakarta.persistence.OptimisticLockException.class);

        verify(eventPublisher, never()).publishEvent(any(DeliveryDispatchExhaustedEvent.class));
    }

    @Test
    @DisplayName("markDispatchExhausted returns false and skips event when order is already terminal (Fix H4)")
    void markDispatchExhausted_alreadyTerminalOrder_returnsFalseAndSkipsEvent() {
        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setOrderId(orderId);
        dispatch.setStatus(DeliveryDispatchStatus.FINDING_AGENT);

        order.setCurrentStatus(OrderStatus.CANCELLED);

        when(orderDispatchRepository.findByOrderId(orderId)).thenReturn(Optional.of(dispatch));
        when(deliveryOfferRepository.findByOrderIdAndStatus(orderId, DeliveryOfferStatus.PENDING))
                .thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderLifecycleService.isTerminal(OrderStatus.CANCELLED)).thenReturn(true);

        boolean shouldCancel = lifecycleService.markDispatchExhausted(orderId);

        assertThat(shouldCancel).isFalse();
        assertThat(dispatch.getStatus()).isEqualTo(DeliveryDispatchStatus.EXHAUSTED);
        verify(eventPublisher, never()).publishEvent(any(DeliveryDispatchExhaustedEvent.class));
    }

    @Test
    @DisplayName("assignAgent assigns agent, updates offer to ACCEPTED, dispatch to AGENT_ASSIGNED, and publishes event")
    void assignAgent_success() {
        DeliveryOffer offer = new DeliveryOffer();
        offer.setStatus(DeliveryOfferStatus.PENDING);
        offer.setExpiresAt(Instant.now().plusSeconds(60));

        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setStatus(DeliveryDispatchStatus.FINDING_AGENT);

        when(userRepository.findById(agentUserId)).thenReturn(Optional.of(agentUser));
        when(deliveryAgentRepository.findByUser(agentUser)).thenReturn(Optional.of(agent));
        when(deliveryAgentRepository.findByIdForUpdate(agent.getId())).thenReturn(Optional.of(agent));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(orderDispatchRepository.findByOrderIdForUpdate(orderId)).thenReturn(Optional.of(dispatch));
        when(deliveryOfferRepository.findByOrderIdAndAgentIdAndStatus(orderId, agent.getId(), DeliveryOfferStatus.PENDING))
                .thenReturn(Optional.of(offer));

        lifecycleService.assignAgent(orderId, agentUserId);

        assertThat(offer.getStatus()).isEqualTo(DeliveryOfferStatus.ACCEPTED);
        assertThat(order.getDeliveryAgent()).isEqualTo(agent);
        assertThat(dispatch.getStatus()).isEqualTo(DeliveryDispatchStatus.AGENT_ASSIGNED);
        assertThat(agent.isAssigned()).isTrue();

        verify(eventPublisher).publishEvent(any(DeliveryAgentAssignedEvent.class));
    }

    @Test
    @DisplayName("assignAgent marks offer EXPIRED, saves it, and throws OfferExpiredException when offer has expired")
    void assignAgent_expiredOffer_throwsOfferExpiredException() {
        DeliveryOffer offer = new DeliveryOffer();
        offer.setStatus(DeliveryOfferStatus.PENDING);
        offer.setExpiresAt(Instant.now().minusSeconds(10));

        OrderDispatch dispatch = new OrderDispatch();
        dispatch.setStatus(DeliveryDispatchStatus.FINDING_AGENT);

        when(userRepository.findById(agentUserId)).thenReturn(Optional.of(agentUser));
        when(deliveryAgentRepository.findByUser(agentUser)).thenReturn(Optional.of(agent));
        when(deliveryAgentRepository.findByIdForUpdate(agent.getId())).thenReturn(Optional.of(agent));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(orderDispatchRepository.findByOrderIdForUpdate(orderId)).thenReturn(Optional.of(dispatch));
        when(deliveryOfferRepository.findByOrderIdAndAgentIdAndStatus(orderId, agent.getId(), DeliveryOfferStatus.PENDING))
                .thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> lifecycleService.assignAgent(orderId, agentUserId))
                .isInstanceOf(com.quickbite.quickbite.delivery.exception.OfferExpiredException.class)
                .hasMessageContaining("expired");

        assertThat(offer.getStatus()).isEqualTo(DeliveryOfferStatus.EXPIRED);
        assertThat(offer.getRespondedAt()).isNotNull();
        verify(deliveryOfferRepository).save(offer);
    }
}
