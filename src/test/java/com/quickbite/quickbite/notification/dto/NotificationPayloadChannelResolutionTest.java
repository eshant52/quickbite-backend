package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.CuisineNotificationType;
import com.quickbite.quickbite.notification.model.DeliveryAgentApplicationNotificationType;
import com.quickbite.quickbite.notification.model.OrderNotificationType;
import com.quickbite.quickbite.notification.model.PaymentNotificationType;
import com.quickbite.quickbite.notification.model.RestaurantApplicationNotificationType;
import com.quickbite.quickbite.notification.model.VehicleApplicationNotificationType;
import com.quickbite.quickbite.notification.service.strategy.NotificationChannel;
import com.quickbite.quickbite.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationPayloadChannelResolutionTest {

    private final User mockUser = new User();

    @Test
    @DisplayName("OrderNotificationPayload: intermediate tracking excludes email; terminal states include all")
    void orderNotificationPayload_channels() {
        // Intermediate updates should NOT spam email
        OrderNotificationPayload preparing = new OrderNotificationPayload(
                mockUser, "Preparing", "Food is being prepared", OrderNotificationType.PREPARING, null);
        assertThat(preparing.targetChannels())
                .containsExactlyInAnyOrder(NotificationChannel.IN_APP, NotificationChannel.PUSH)
                .doesNotContain(NotificationChannel.EMAIL);

        OrderNotificationPayload assigned = new OrderNotificationPayload(
                mockUser, "Assigned", "Driver assigned", OrderNotificationType.AGENT_ASSIGNED, null);
        assertThat(assigned.targetChannels())
                .containsExactlyInAnyOrder(NotificationChannel.IN_APP, NotificationChannel.PUSH)
                .doesNotContain(NotificationChannel.EMAIL);

        // Terminal / critical milestones should include all channels
        OrderNotificationPayload placed = new OrderNotificationPayload(
                mockUser, "Placed", "Order confirmed", OrderNotificationType.PLACED, null);
        assertThat(placed.targetChannels()).isEqualTo(EnumSet.allOf(NotificationChannel.class));

        OrderNotificationPayload delivered = new OrderNotificationPayload(
                mockUser, "Delivered", "Food delivered", OrderNotificationType.DELIVERED, null);
        assertThat(delivered.targetChannels()).isEqualTo(EnumSet.allOf(NotificationChannel.class));
    }

    @Test
    @DisplayName("PaymentNotificationPayload: success excludes push (handled by order placed); failure excludes email")
    void paymentNotificationPayload_channels() {
        PaymentNotificationPayload success = new PaymentNotificationPayload(
                mockUser, "Paid", "Success", PaymentNotificationType.SUCCESS, null);
        assertThat(success.targetChannels())
                .containsExactlyInAnyOrder(NotificationChannel.IN_APP, NotificationChannel.EMAIL)
                .doesNotContain(NotificationChannel.PUSH);

        PaymentNotificationPayload failed = new PaymentNotificationPayload(
                mockUser, "Failed", "Failed", PaymentNotificationType.FAILED, null);
        assertThat(failed.targetChannels())
                .containsExactlyInAnyOrder(NotificationChannel.IN_APP, NotificationChannel.PUSH)
                .doesNotContain(NotificationChannel.EMAIL);

        PaymentNotificationPayload refunded = new PaymentNotificationPayload(
                mockUser, "Refunded", "Refunded", PaymentNotificationType.REFUNDED, null);
        assertThat(refunded.targetChannels()).isEqualTo(EnumSet.allOf(NotificationChannel.class));
    }

    @Test
    @DisplayName("Admin review allotment payloads: only IN_APP to avoid mobile push / email noise")
    void adminAllotment_onlyInApp() {
        CuisineNotificationPayload cuisine = new CuisineNotificationPayload(
                mockUser, "Review", "Review cuisine", UUID.randomUUID(), "Tacos", CuisineNotificationType.PENDING);
        assertThat(cuisine.targetChannels()).containsExactly(NotificationChannel.IN_APP);

        RestaurantApplicationNotificationPayload restaurant = new RestaurantApplicationNotificationPayload(
                mockUser, "Review", "Review rest", RestaurantApplicationNotificationType.APPLICATION_SUBMITTED, UUID.randomUUID(), "Diner");
        assertThat(restaurant.targetChannels()).containsExactly(NotificationChannel.IN_APP);

        DeliveryAgentApplicationNotificationPayload agent = new DeliveryAgentApplicationNotificationPayload(
                mockUser, "Review", "Review agent", DeliveryAgentApplicationNotificationType.APPLICATION_SUBMITTED, UUID.randomUUID(), "Dave");
        assertThat(agent.targetChannels()).containsExactly(NotificationChannel.IN_APP);

        VehicleApplicationNotificationPayload vehicle = new VehicleApplicationNotificationPayload(
                mockUser, "Review", "Review vehicle", VehicleApplicationNotificationType.APPLICATION_SUBMITTED, UUID.randomUUID(), "Bike");
        assertThat(vehicle.targetChannels()).containsExactly(NotificationChannel.IN_APP);
    }

    @Test
    @DisplayName("Partner approval/rejection payloads: deliver across all channels for milestone clarity")
    void partnerDecision_allChannels() {
        RestaurantApplicationNotificationPayload approved = new RestaurantApplicationNotificationPayload(
                mockUser, "Approved", "Live", RestaurantApplicationNotificationType.APPLICATION_APPROVED, UUID.randomUUID(), "Diner");
        assertThat(approved.targetChannels()).isEqualTo(EnumSet.allOf(NotificationChannel.class));

        DeliveryAgentApplicationNotificationPayload rejected = new DeliveryAgentApplicationNotificationPayload(
                mockUser, "Rejected", "Needs changes", DeliveryAgentApplicationNotificationType.APPLICATION_REJECTED, UUID.randomUUID(), "Dave");
        assertThat(rejected.targetChannels()).isEqualTo(EnumSet.allOf(NotificationChannel.class));
    }
}
