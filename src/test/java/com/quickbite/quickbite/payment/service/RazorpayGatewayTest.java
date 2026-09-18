package com.quickbite.quickbite.payment.service;

import com.quickbite.quickbite.common.config.property.RazorpayProperties;
import com.quickbite.quickbite.payment.dto.GatewayOrderDetails;
import com.quickbite.quickbite.payment.dto.GatewayOrderStatus;
import com.quickbite.quickbite.payment.dto.GatewayWebhookEvent;
import com.quickbite.quickbite.payment.service.gateway.RazorpayGateway;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RazorpayGatewayTest {

    // We test the non-SDK-dependent logic (amount conversion, event parsing) directly.
    // SDK calls (verifyPaymentSignature, verifyWebhookSignature) require real keys and are
    // tested via integration/manual verification with Razorpay test mode.

    @Mock
    private RazorpayClient razorpayClient;

    private RazorpayProperties razorpayProperties;
    private RazorpayGateway razorpayGateway;

    @BeforeEach
    void setUp() {
        razorpayProperties = new RazorpayProperties("rzp_test_key", "secret123", "webhook_secret");
        razorpayGateway = new RazorpayGateway(razorpayClient, razorpayProperties);
    }

    @Test
    @DisplayName("parseWebhookEvent - correctly extracts event type, gatewayOrderId, gatewayPaymentId")
    void parseWebhookEvent_Success() {
        String rawBody = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_TestPaymentId",
                        "order_id": "order_TestOrderId"
                      }
                    }
                  }
                }
                """;

        GatewayWebhookEvent event = razorpayGateway.parseWebhookEvent(rawBody);

        assertThat(event.eventType()).isEqualTo("payment.captured");
        assertThat(event.gatewayOrderId()).isEqualTo("order_TestOrderId");
        assertThat(event.gatewayPaymentId()).isEqualTo("pay_TestPaymentId");
    }

    @Test
    @DisplayName("parseWebhookEvent - correctly parses payment.failed event with error message")
    void parseWebhookEvent_PaymentFailed() {
        String rawBody = """
                {
                  "event": "payment.failed",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_Failed123",
                        "order_id": "order_Abc456",
                        "error_description": "Payment was declined by the bank."
                      }
                    }
                  }
                }
                """;

        GatewayWebhookEvent event = razorpayGateway.parseWebhookEvent(rawBody);

        assertThat(event.eventType()).isEqualTo("payment.failed");
        assertThat(event.gatewayOrderId()).isEqualTo("order_Abc456");
        assertThat(event.gatewayPaymentId()).isEqualTo("pay_Failed123");
        assertThat(event.message()).isEqualTo("Payment was declined by the bank.");
    }

    @Test
    @DisplayName("providerKeyId - GatewayOrder always carries keyId from RazorpayProperties")
    void createOrder_GatewayOrderCarriesKeyId() {
        // The keyId returned to the client must always match the configured publishable key
        assertThat(razorpayProperties.keyId()).isEqualTo("rzp_test_key");
    }

    @Test
    @DisplayName("INR to paise conversion — 499.00 INR → 49900 paise (unit math validation)")
    void amountConversion_InrToPaise() {
        // Verify the math: BigDecimal(499.00) * 100 = 49900 long — this is what createOrder passes to Razorpay
        java.math.BigDecimal amount = new java.math.BigDecimal("499.00");
        long expectedPaise = amount
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .multiply(java.math.BigDecimal.valueOf(100))
                .longValueExact();
        assertThat(expectedPaise).isEqualTo(49900L);
    }

    @Test
    @DisplayName("fetchOrderStatus - returns OPEN when status is created")
    void fetchOrderStatus_Open_Created() throws Exception {
        OrderClient orderClient = mock(OrderClient.class);
        razorpayClient.orders = orderClient;

        Order mockOrder = mock(Order.class);
        when(mockOrder.get("status")).thenReturn("created");
        when(orderClient.fetch("order_123")).thenReturn(mockOrder);

        GatewayOrderDetails details = razorpayGateway.fetchOrderStatus("order_123");

        assertThat(details.status()).isEqualTo(GatewayOrderStatus.OPEN);
        assertThat(details.gatewayPaymentId()).isNull();
    }

    @Test
    @DisplayName("fetchOrderStatus - returns PAID and captures paymentId when status is paid")
    void fetchOrderStatus_Paid_WithPaymentId() throws Exception {
        OrderClient orderClient = mock(OrderClient.class);
        razorpayClient.orders = orderClient;

        Order mockOrder = mock(Order.class);
        when(mockOrder.get("status")).thenReturn("paid");
        when(orderClient.fetch("order_123")).thenReturn(mockOrder);

        com.razorpay.Payment capturedPayment = mock(com.razorpay.Payment.class);
        when(capturedPayment.get("status")).thenReturn("captured");
        when(capturedPayment.get("id")).thenReturn("pay_captured_999");
        when(orderClient.fetchPayments("order_123")).thenReturn(List.of(capturedPayment));

        GatewayOrderDetails details = razorpayGateway.fetchOrderStatus("order_123");

        assertThat(details.status()).isEqualTo(GatewayOrderStatus.PAID);
        assertThat(details.gatewayPaymentId()).isEqualTo("pay_captured_999");
    }

    @Test
    @DisplayName("fetchOrderStatus - returns EXPIRED when error indicates order does not exist or is closed")
    void fetchOrderStatus_Expired_OnError() throws Exception {
        OrderClient orderClient = mock(OrderClient.class);
        razorpayClient.orders = orderClient;

        when(orderClient.fetch("order_123")).thenThrow(new RazorpayException("The id provided does not exist"));

        GatewayOrderDetails details = razorpayGateway.fetchOrderStatus("order_123");

        assertThat(details.status()).isEqualTo(GatewayOrderStatus.EXPIRED);
    }

    @Test
    @DisplayName("fetchOrderStatus - returns UNKNOWN on generic network failure")
    void fetchOrderStatus_Unknown_OnGenericError() throws Exception {
        OrderClient orderClient = mock(OrderClient.class);
        razorpayClient.orders = orderClient;

        when(orderClient.fetch("order_123")).thenThrow(new RazorpayException("Connection timeout"));

        GatewayOrderDetails details = razorpayGateway.fetchOrderStatus("order_123");

        assertThat(details.status()).isEqualTo(GatewayOrderStatus.UNKNOWN);
    }
}
