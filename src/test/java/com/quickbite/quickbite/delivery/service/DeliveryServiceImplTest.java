package com.quickbite.quickbite.delivery.service;

import com.quickbite.quickbite.common.event.order.OrderStatusChangedEvent;
import com.quickbite.quickbite.common.event.payment.PaymentStatusChangedEvent;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.quickbite.delivery.dto.UpdateLocationRequest;
import com.quickbite.quickbite.delivery.exception.DeliveryAgentNotFoundException;
import com.quickbite.quickbite.delivery.exception.NoAvailableDeliveryAgentException;
import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.model.DeliveryAgentVerificationStatus;
import com.quickbite.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.quickbite.delivery.service.strategy.DeliveryAssignmentStrategy;
import com.quickbite.quickbite.order.dto.OrderResponse;
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
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    @Mock
    private DeliveryAgentRepository deliveryAgentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentStatusHistoryRepository paymentStatusHistoryRepository;

    @Mock
    private DeliveryAssignmentStrategy deliveryAssignmentStrategy;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DeliveryServiceImpl deliveryService;

    private User user;
    private DeliveryAgent agent;
    private Order order;
    private UUID userId;
    private UUID agentId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        agentId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setName("Rider Jack");
        user.setEmail("jack@delivery.com");
        user.setPhoneNumber("9876543210");

        agent = new DeliveryAgent();
        agent.setId(agentId);
        agent.setUser(user);
        agent.setAvailable(false);
        agent.setCurrentStatus(DeliveryAgentVerificationStatus.APPROVED);
        agent.setCreatedAt(Instant.now());

        Restaurant restaurant = new Restaurant();
        restaurant.setId(UUID.randomUUID());
        restaurant.setName("Burger Hub");

        order = new Order();
        order.setId(orderId);
        order.setCustomer(user);
        order.setRestaurant(restaurant);
        order.setItems(new ArrayList<>());
        order.setCurrentStatus(OrderStatus.READY_FOR_PICKUP);
        order.setTotalAmount(BigDecimal.valueOf(250.00));
        order.setCreatedAt(Instant.now());
    }

    @Nested
    @DisplayName("getMyProfile")
    class ProfileTests {

        @Test
        @DisplayName("Returns delivery agent profile when found")
        void getMyProfile_success() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(deliveryAgentRepository.findByUser(user)).thenReturn(Optional.of(agent));

            DeliveryAgentResponse res = deliveryService.getMyProfile(userId);

            assertThat(res.userId()).isEqualTo(userId);
            assertThat(res.userName()).isEqualTo("Rider Jack");
        }

        @Test
        @DisplayName("Throws DeliveryAgentNotFoundException when not found")
        void getMyProfile_notFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(deliveryAgentRepository.findByUser(user)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryService.getMyProfile(userId))
                    .isInstanceOf(DeliveryAgentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateLocation")
    class UpdateLocationTests {

        @Test
        @DisplayName("Updates GPS point and preserves existing availability and assigned status")
        void updateLocation_approvedAgent() {
            agent.setCurrentStatus(DeliveryAgentVerificationStatus.APPROVED);
            agent.setAvailable(true);
            agent.setAssigned(true); // Agent is on an active delivery
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(deliveryAgentRepository.findByUser(user)).thenReturn(Optional.of(agent));
            when(deliveryAgentRepository.save(any(DeliveryAgent.class))).thenAnswer(i -> i.getArgument(0));

            UpdateLocationRequest req = new UpdateLocationRequest(12.9716, 77.5946);
            DeliveryAgentResponse res = deliveryService.updateLocation(userId, req);

            assertThat(res.isAvailable()).isTrue();
            assertThat(res.isAssigned()).isTrue();
            assertThat(res.latitude()).isEqualTo(12.9716);
            assertThat(res.longitude()).isEqualTo(77.5946);
        }
    }

    @Nested
    @DisplayName("updateAvailability")
    class AvailabilityTests {

        @Test
        @DisplayName("Approved agent can go online")
        void updateAvailability_goOnline_success() {
            agent.setCurrentStatus(DeliveryAgentVerificationStatus.APPROVED);
            agent.setAvailable(false);
            agent.setAssigned(false);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(deliveryAgentRepository.findByUser(user)).thenReturn(Optional.of(agent));
            when(deliveryAgentRepository.save(any(DeliveryAgent.class))).thenAnswer(i -> i.getArgument(0));

            DeliveryAgentResponse res = deliveryService.updateAvailability(userId, true);

            assertThat(res.isAvailable()).isTrue();
            assertThat(agent.isAvailable()).isTrue();
            verify(deliveryAgentRepository).save(agent);
        }

        @Test
        @DisplayName("Approved agent can go offline when not assigned an order")
        void updateAvailability_goOffline_success() {
            agent.setCurrentStatus(DeliveryAgentVerificationStatus.APPROVED);
            agent.setAvailable(true);
            agent.setAssigned(false);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(deliveryAgentRepository.findByUser(user)).thenReturn(Optional.of(agent));
            when(deliveryAgentRepository.save(any(DeliveryAgent.class))).thenAnswer(i -> i.getArgument(0));

            DeliveryAgentResponse res = deliveryService.updateAvailability(userId, false);

            assertThat(res.isAvailable()).isFalse();
            assertThat(agent.isAvailable()).isFalse();
            verify(deliveryAgentRepository).save(agent);
        }

        @Test
        @DisplayName("Throws BadRequestException if agent tries to go offline while carrying an active order")
        void updateAvailability_cannotGoOfflineWhenAssigned() {
            agent.setCurrentStatus(DeliveryAgentVerificationStatus.APPROVED);
            agent.setAvailable(true);
            agent.setAssigned(true); // busy with order!

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(deliveryAgentRepository.findByUser(user)).thenReturn(Optional.of(agent));

            assertThatThrownBy(() -> deliveryService.updateAvailability(userId, false))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Cannot go off-duty while you have an active delivery");

            verify(deliveryAgentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws BadRequestException if unapproved agent tries to update availability")
        void updateAvailability_unapprovedThrows() {
            agent.setCurrentStatus(DeliveryAgentVerificationStatus.PENDING);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(deliveryAgentRepository.findByUser(user)).thenReturn(Optional.of(agent));

            assertThatThrownBy(() -> deliveryService.updateAvailability(userId, true))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Only approved delivery agents");

            verify(deliveryAgentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("autoAssign")
    class AutoAssignTests {

        @Test
        @DisplayName("Assigns available nearest agent to order and marks agent isAssigned=true")
        void autoAssign_success() {
            agent.setCurrentStatus(DeliveryAgentVerificationStatus.APPROVED);
            agent.setAvailable(true);
            agent.setAssigned(false);
            when(deliveryAssignmentStrategy.findAgent(order)).thenReturn(Optional.of(agent));

            deliveryService.autoAssign(order);

            assertThat(order.getDeliveryAgent()).isEqualTo(agent);
            assertThat(agent.isAssigned()).isTrue();
            assertThat(agent.isAvailable()).isTrue(); // Duty status remains untouched
            verify(orderRepository).save(order);
            verify(deliveryAgentRepository).save(agent);
        }

        @Test
        @DisplayName("Throws NoAvailableDeliveryAgentException when no agent found")
        void autoAssign_noneFound() {
            when(deliveryAssignmentStrategy.findAgent(order)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryService.autoAssign(order))
                    .isInstanceOf(NoAvailableDeliveryAgentException.class);
        }
    }

    @Nested
    @DisplayName("markOutForDelivery & markDelivered")
    class OrderLifecycleTests {

        @Test
        @DisplayName("markOutForDelivery transitions order from READY_FOR_PICKUP to OUT_FOR_DELIVERY")
        void markOutForDelivery_success() {
            order.setDeliveryAgent(agent);
            order.setCurrentStatus(OrderStatus.READY_FOR_PICKUP);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(deliveryAgentRepository.findByUser(user)).thenReturn(Optional.of(agent));
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

            OrderResponse res = deliveryService.markOutForDelivery(orderId, userId);

            assertThat(res.currentStatus()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);
            verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
            verify(eventPublisher).publishEvent(any(OrderStatusChangedEvent.class));
        }

        @Test
        @DisplayName("markDelivered transitions to DELIVERED, frees agent, and collects COD payment")
        void markDelivered_codSuccess() {
            order.setDeliveryAgent(agent);
            order.setCurrentStatus(OrderStatus.OUT_FOR_DELIVERY);

            Payment codPayment = new Payment();
            codPayment.setId(UUID.randomUUID());
            codPayment.setPaymentMethod(PaymentMethod.COD);
            codPayment.setCurrentStatus(PaymentStatus.PENDING);
            codPayment.setAmount(BigDecimal.valueOf(250.00));

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(deliveryAgentRepository.findByUser(user)).thenReturn(Optional.of(agent));
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
            when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(codPayment));
            agent.setAssigned(true);

            OrderResponse res = deliveryService.markDelivered(orderId, userId);

            assertThat(res.currentStatus()).isEqualTo(OrderStatus.DELIVERED);
            assertThat(agent.isAssigned()).isFalse();
            assertThat(codPayment.getCurrentStatus()).isEqualTo(PaymentStatus.SUCCESS);

            verify(deliveryAgentRepository).save(agent);
            verify(paymentRepository).save(codPayment);
            verify(paymentStatusHistoryRepository).save(any(PaymentStatusHistory.class));
            verify(eventPublisher).publishEvent(any(PaymentStatusChangedEvent.class));
            verify(eventPublisher).publishEvent(any(OrderStatusChangedEvent.class));
        }
    }
}
