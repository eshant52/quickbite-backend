package com.quickbite.quickbite.payment.service.gateway;

import com.quickbite.quickbite.common.config.property.RazorpayProperties;
import com.quickbite.quickbite.payment.dto.GatewayOrder;
import com.quickbite.quickbite.payment.dto.GatewayOrderDetails;
import com.quickbite.quickbite.payment.dto.GatewayOrderStatus;
import com.quickbite.quickbite.payment.dto.GatewayWebhookEvent;
import com.quickbite.quickbite.payment.dto.RazorpayGatewayWebhookEvent;
import com.quickbite.quickbite.payment.exception.PaymentVerificationException;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Razorpay implementation of {@link PaymentGateway}.
 *
 * <p>Wraps the Razorpay Java SDK behind the clean {@link PaymentGateway} interface
 * so the rest of the application never imports Razorpay SDK classes directly.
 * Replace or supplement with {@code StripeGateway} in the future without touching
 * any strategy or service code.
 *
 * <p>Key Razorpay concepts mapped to internal DTOs:
 * <ul>
 *   <li>{@code order_xxx} → {@link GatewayOrder#gatewayOrderId()}</li>
 *   <li>{@code pay_xxx}   → {@link GatewayWebhookEvent#gatewayPaymentId()}</li>
 *   <li>keyId (publishable) → {@link GatewayOrder#providerKeyId()}</li>
 * </ul>
 */
@Primary
@Component
public class RazorpayGateway implements PaymentGateway {

    private static final String NAME = "Razorpay";

    private static final Logger log = LoggerFactory.getLogger(RazorpayGateway.class);

    private static final String CURRENCY_INR = "INR";
    private static final int PAISE_MULTIPLIER = 100;

    private final RazorpayClient razorpayClient;
    private final RazorpayProperties razorpayProperties;

    public RazorpayGateway(RazorpayClient razorpayClient, RazorpayProperties razorpayProperties) {
        this.razorpayClient = razorpayClient;
        this.razorpayProperties = razorpayProperties;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getPublishableKey() {
        return razorpayProperties.keyId();
    }

    /**
     * Creates a Razorpay order. Converts the INR {@link BigDecimal} amount to paise
     * (Razorpay requires amounts as integer paise, e.g. ₹499.00 → 49900).
     */
    @Override
    public GatewayOrder createOrder(BigDecimal amount, String receipt) {
        try {
            long amountInPaise = amount
                    .setScale(2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(PAISE_MULTIPLIER))
                    .longValueExact();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", CURRENCY_INR);
            orderRequest.put("receipt", receipt);

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            log.debug("Created Razorpay order {} for receipt {}", razorpayOrderId, receipt);
            return new GatewayOrder(razorpayOrderId, razorpayProperties.keyId());

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for receipt {}: {}", receipt, e.getMessage());
            throw new RuntimeException("Payment gateway order creation failed", e);
        }
    }

    /**
     * Verifies the HMAC-SHA256 signature returned by Razorpay Checkout JS.
     * Razorpay signs: HMAC-SHA256(razorpay_order_id + "|" + razorpay_payment_id, keySecret).
     */
    @Override
    public void verifyPaymentSignature(String gatewayOrderId, String gatewayPaymentId, String signature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", gatewayOrderId);
            options.put("razorpay_payment_id", gatewayPaymentId);
            options.put("razorpay_signature", signature);

            boolean valid = Utils.verifyPaymentSignature(options, razorpayProperties.keySecret());
            if (!valid) {
                throw new PaymentVerificationException("Razorpay payment signature verification failed");
            }
        } catch (RazorpayException e) {
            throw new PaymentVerificationException("Razorpay payment signature verification failed: " + e.getMessage());
        }
    }

    /**
     * Verifies the HMAC-SHA256 webhook signature from the {@code X-Razorpay-Signature} header.
     * Must receive the raw, unparsed body — any re-serialization breaks the hash.
     */
    @Override
    public void verifyWebhookSignature(String rawBody, String signature) {
        try {
            boolean valid = Utils.verifyWebhookSignature(rawBody, signature, razorpayProperties.webhookSecret());
            if (!valid) {
                throw new PaymentVerificationException("Razorpay webhook signature verification failed");
            }
        } catch (RazorpayException e) {
            throw new PaymentVerificationException("Razorpay webhook signature verification failed: " + e.getMessage());
        }
    }

    /**
     * Parses a Razorpay webhook JSON body into a {@link GatewayWebhookEvent}.
     *
     * <p>Razorpay webhook shape (simplified):
     * <pre>{@code
     * {
     *   "event": "payment.captured",
     *   "payload": {
     *     "payment": {
     *       "entity": {
     *         "id": "pay_xxx",
     *         "order_id": "order_xxx",
     *         "error_description": "Payment failed due to insufficient funds",
     *         "error_reason": "Insufficient funds"
     *       }
     *     }
     *   }
     * }
     * }</pre>
     */
    @Override
    public GatewayWebhookEvent parseWebhookEvent(String rawBody) {
        JSONObject body = new JSONObject(rawBody);
        String eventType = body.optString("event", "unknown");

        if (!body.has("payload") || !body.getJSONObject("payload").has("payment")) {
            log.debug("Razorpay webhook payload does not contain payment entity: {}", eventType);
            return new RazorpayGatewayWebhookEvent(eventType, null, null, null);
        }

        JSONObject paymentWrapper = body.getJSONObject("payload").getJSONObject("payment");
        if (!paymentWrapper.has("entity")) {
            return new RazorpayGatewayWebhookEvent(eventType, null, null, null);
        }

        JSONObject paymentEntity = paymentWrapper.getJSONObject("entity");
        String gatewayPaymentId = paymentEntity.optString("id", null);
        String gatewayOrderId = paymentEntity.optString("order_id", null);

        // Extract failure message / error description if present (e.g. on payment.failed)
        String message = null;
        if (paymentEntity.has("error_description") && !paymentEntity.isNull("error_description")) {
            message = paymentEntity.getString("error_description");
        } else if (paymentEntity.has("error_reason") && !paymentEntity.isNull("error_reason")) {
            message = paymentEntity.getString("error_reason");
        }

        return new RazorpayGatewayWebhookEvent(eventType, gatewayOrderId, gatewayPaymentId, message);
    }

    @Override
    public void refund(String gatewayPaymentId, BigDecimal amount, String reason) {
        try {
            JSONObject refundRequest = getRefundRequest(amount, reason);

            razorpayClient.payments.refund(gatewayPaymentId, refundRequest);
            log.info("Successfully initiated Razorpay refund for payment {} (amount: ₹{})", gatewayPaymentId, amount);
        } catch (RazorpayException e) {
            log.error("Failed to initiate Razorpay refund for payment {}: {}", gatewayPaymentId, e.getMessage(), e);
            throw new PaymentVerificationException("Failed to process refund: " + e.getMessage());
        }
    }

    private static @NonNull JSONObject getRefundRequest(BigDecimal amount, String reason) {
        long amountInPaise = amount
                .setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(PAISE_MULTIPLIER))
                .longValueExact();

        JSONObject refundRequest = new JSONObject();
        refundRequest.put("amount", amountInPaise);
        if (reason != null && !reason.isBlank()) {
            JSONObject notes = new JSONObject();
            notes.put("reason", reason);
            refundRequest.put("notes", notes);
        }
        return refundRequest;
    }

    @Override
    public GatewayOrderDetails fetchOrderStatus(String gatewayOrderId) {
        try {
            com.razorpay.Order order = razorpayClient.orders.fetch(gatewayOrderId);
            String status = order.get("status");
            if (status == null) {
                return GatewayOrderDetails.of(GatewayOrderStatus.UNKNOWN);
            }

            return switch (status.toLowerCase()) {
                case "created", "attempted" -> GatewayOrderDetails.of(GatewayOrderStatus.OPEN);
                case "paid" -> {
                    String capturedPaymentId = null;
                    try {
                        List<com.razorpay.Payment> payments = razorpayClient.orders.fetchPayments(gatewayOrderId);
                        if (payments != null) {
                            for (com.razorpay.Payment p : payments) {
                                if ("captured".equalsIgnoreCase(p.get("status"))) {
                                    capturedPaymentId = p.get("id");
                                    break;
                                }
                            }
                        }
                    } catch (Exception ex) {
                        log.warn("Could not fetch payments for paid Razorpay order {}: {}", gatewayOrderId, ex.getMessage());
                    }
                    yield new GatewayOrderDetails(GatewayOrderStatus.PAID, capturedPaymentId);
                }
                default -> GatewayOrderDetails.of(GatewayOrderStatus.EXPIRED);
            };
        } catch (RazorpayException e) {
            log.error("Failed to fetch Razorpay order {}: {}", gatewayOrderId, e.getMessage());
            if (e.getMessage() != null && (e.getMessage().toLowerCase().contains("does not exist")
                    || e.getMessage().toLowerCase().contains("closed"))) {
                return GatewayOrderDetails.of(GatewayOrderStatus.EXPIRED);
            }
            return GatewayOrderDetails.of(GatewayOrderStatus.UNKNOWN);
        }
    }
}
