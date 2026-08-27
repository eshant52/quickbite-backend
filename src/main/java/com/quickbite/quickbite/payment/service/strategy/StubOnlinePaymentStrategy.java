package com.quickbite.quickbite.payment.service.strategy;

import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.dto.StubOnlinePaymentResult;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;
import com.quickbite.quickbite.payment.model.PaymentStatusHistory;
import com.quickbite.quickbite.payment.repository.PaymentRepository;
import com.quickbite.quickbite.payment.repository.PaymentStatusHistoryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stub strategy for all online payment methods (UPI, CARD, NET_BANKING, WALLET).
 *
 * <p><b>Transaction contract (TX 2 of checkout):</b>
 * <ol>
 *   <li>Creates a {@link Payment} record (status = PENDING).</li>
 *   <li>Records a {@link PaymentStatusHistory} entry.</li>
 *   <li>Returns a {@link StubOnlinePaymentResult} with a fake {@code paymentUrl}
 *       that the client can redirect to for local development.</li>
 * </ol>
 *
 * <p>The cart is <b>not</b> cleared here — it remains alive until the payment
 * gateway webhook confirms success. If payment fails or times out, the customer
 * can retry without losing their cart.
 *
 * <p>No Kafka event is published here. {@code OrderPlacedEvent} will be fired
 * by the webhook handler (once implemented) after the payment is confirmed.
 *
 * <p>Replace this with {@code RazorpayPaymentStrategy} / {@code StripePaymentStrategy}
 * once the real gateway adapters are implemented. Those strategies should:
 * <ol>
 *   <li>Persist the Payment (PENDING) in a fast DB transaction.</li>
 *   <li>Call the gateway HTTP API <em>outside</em> any DB transaction (no connection held).</li>
 *   <li>On success: update payment (SUCCESS), transition order (PLACED), clear cart,
 *       publish {@code OrderPlacedEvent} — all in a new transaction.</li>
 *   <li>On failure: update payment (FAILED), transition order (PAYMENT_FAILED) — new transaction.</li>
 * </ol>
 */
@Component
public class StubOnlinePaymentStrategy implements PaymentStrategy {

    private final PaymentRepository paymentRepository;
    private final PaymentStatusHistoryRepository paymentStatusHistoryRepository;

    public StubOnlinePaymentStrategy(
            PaymentRepository paymentRepository,
            PaymentStatusHistoryRepository paymentStatusHistoryRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentStatusHistoryRepository = paymentStatusHistoryRepository;
    }

    @Override
    @Transactional
    public PaymentResult initiate(Order order, PaymentMethod paymentMethod) {

        // 1. Persist Payment record (PENDING — awaiting gateway confirmation)
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(paymentMethod);
        payment.setTransactionId("STUB-" + order.getId().toString().substring(0, 8).toUpperCase());
        payment.setAmount(order.getTotalAmount());
        payment.setCurrentStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        // 2. Persist payment status history
        PaymentStatusHistory history = new PaymentStatusHistory();
        history.setPayment(payment);
        history.setStatus(PaymentStatus.PENDING);
        paymentStatusHistoryRepository.save(history);

        // 3. Return stub result — real strategies would return gateway-specific credentials
        //    (Razorpay: gatewayOrderId + keyId; Stripe: clientSecret + publishableKey)
        String stubPaymentUrl = "https://stub-gateway.quickbite.local/pay?txn=" + payment.getTransactionId();
        return new StubOnlinePaymentResult(
                payment.getId(),
                order.getId(),
                payment.getTransactionId(),
                paymentMethod,
                PaymentStatus.PENDING,
                order.getTotalAmount(),
                stubPaymentUrl
        );
    }

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return paymentMethod.isOnline();
    }
}
