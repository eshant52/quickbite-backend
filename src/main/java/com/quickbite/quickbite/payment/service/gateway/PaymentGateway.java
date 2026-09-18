package com.quickbite.quickbite.payment.service.gateway;

import com.quickbite.quickbite.payment.dto.GatewayOrder;
import com.quickbite.quickbite.payment.dto.GatewayOrderDetails;
import com.quickbite.quickbite.payment.dto.GatewayWebhookEvent;
import com.quickbite.quickbite.payment.exception.PaymentVerificationException;

import java.math.BigDecimal;

/**
 * Adapter interface for external payment gateway providers.
 *
 * <p>{@link com.quickbite.quickbite.payment.service.strategy.OnlinePaymentStrategy}
 * codes against this interface only — it never touches gateway SDK classes directly.
 * Today {@link RazorpayGateway} is the sole implementation; adding Stripe requires
 * only a new {@code StripeGateway} bean with zero changes to the strategy.
 */
public interface PaymentGateway {

    /**
     * Returns the human-readable name of this gateway (e.g. "Razorpay").
     */
    String getName();

    /**
     * Returns the client-facing publishable key for this gateway.
     */
    String getPublishableKey();

    /**
     * Creates a gateway-side order and returns credentials the client needs to open
     * the gateway's Checkout UI (e.g. Razorpay Checkout JS).
     *
     * @param amount  Order total in INR (BigDecimal, e.g. 499.00). Implementations
     *                are responsible for unit conversion (e.g. INR → paise for Razorpay).
     * @param receipt Unique identifier for this payment attempt (typically the QuickBite
     *                order UUID). Used by the gateway for deduplication and audit logs.
     * @return a {@link GatewayOrder} containing the gateway order ID and publishable key.
     */
    GatewayOrder createOrder(BigDecimal amount, String receipt);

    /**
     * Verifies the HMAC-SHA256 signature returned by the gateway's Checkout UI to the client.
     *
     * <p><b>Must be called with the exact values received from the client</b> — do not
     * transform or re-encode them before passing here.
     *
     * @param gatewayOrderId   Gateway's order ID (Razorpay: razorpay_order_id).
     * @param gatewayPaymentId Gateway's payment ID (Razorpay: razorpay_payment_id).
     * @param signature        HMAC-SHA256 signature (Razorpay: razorpay_signature).
     * @throws PaymentVerificationException if the signature does not match.
     */
    void verifyPaymentSignature(String gatewayOrderId, String gatewayPaymentId, String signature);

    /**
     * Verifies the HMAC-SHA256 signature on an incoming webhook request.
     *
     * <p><b>Must be called with the raw, unparsed request body.</b> Parsing the body to
     * a POJO and re-serializing it will change whitespace and key ordering, breaking the hash.
     *
     * @param rawBody   Raw webhook request body as a UTF-8 string.
     * @param signature Value of the {@code X-Razorpay-Signature} (or equivalent) header.
     * @throws PaymentVerificationException if the signature does not match.
     */
    void verifyWebhookSignature(String rawBody, String signature);

    /**
     * Parses a raw webhook body into a structured {@link GatewayWebhookEvent}.
     *
     * @param rawBody Raw webhook request body as a UTF-8 string.
     * @return parsed event containing type, gateway order ID, and gateway payment ID.
     */
    GatewayWebhookEvent parseWebhookEvent(String rawBody);

    /**
     * Issues a refund for a previously captured payment through the gateway provider.
     *
     * @param gatewayPaymentId Gateway's payment ID (Razorpay: payment_id).
     * @param amount           Refund amount in INR.
     * @param reason           Reason or notes for the refund.
     */
    void refund(String gatewayPaymentId, BigDecimal amount, String reason);

    /**
     * Checks the real-time status of an order on the payment gateway.
     *
     * @param gatewayOrderId Gateway's order ID (Razorpay: order_xxx).
     * @return structured order status and optional captured payment ID.
     */
    GatewayOrderDetails fetchOrderStatus(String gatewayOrderId);
}
