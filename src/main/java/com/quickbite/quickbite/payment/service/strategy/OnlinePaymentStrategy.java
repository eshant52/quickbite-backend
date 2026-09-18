package com.quickbite.quickbite.payment.service.strategy;

import com.quickbite.quickbite.common.utils.TransactionIdGenerator;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.service.OrderLifecycleService;
import com.quickbite.quickbite.payment.dto.GatewayOrder;
import com.quickbite.quickbite.payment.dto.OnlinePaymentResult;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;
import com.quickbite.quickbite.payment.service.PaymentLifecycleService;
import com.quickbite.quickbite.payment.service.gateway.PaymentGateway;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;


/**
 * Payment strategy for all online payment methods (UPI, CARD, NET_BANKING, WALLET).
 *
 * <p><b>Transaction contract (TX 2 of checkout):</b>
 * <ol>
 *  <li>Creates a {@link Payment} record in PENDING state, with initial {@code PaymentStatusHistory}.</li>
 *  <li>Calls the gateway to create a gateway order (HTTP call outside TX).</li>
 *  <li>Updates the {@code Payment} record with the gateway order ID and provider key ID.</li>
 * </ol>
 *
 * <p>The cart is <b>not</b> cleared and {@code OrderPlacedEvent} is <b>not</b> published here.
 * Both happen only after the gateway confirms payment — either via the client-side
 * verify endpoint or the server-side webhook — handled by
 * {@link com.quickbite.quickbite.payment.service.PaymentServiceImpl}.
 *
 * <p>This strategy does not know or care which gateway is used. It injects
 * {@link PaymentGateway} — today {@code RazorpayGateway},
 * tomorrow {@code StripeGateway} — with zero changes here.
 */
@Profile("!dev")
@Component
public class OnlinePaymentStrategy implements PaymentStrategy {

    private final TransactionIdGenerator transactionIdGenerator;
    private final PaymentGateway paymentGateway;
    private final PaymentLifecycleService paymentLifecycle;

    public OnlinePaymentStrategy(
            TransactionIdGenerator transactionIdGenerator,
            PaymentGateway paymentGateway,
            PaymentLifecycleService paymentLifecycle, OrderLifecycleService orderLifecycleService) {
        this.transactionIdGenerator = transactionIdGenerator;
        this.paymentGateway = paymentGateway;
        this.paymentLifecycle = paymentLifecycle;
    }

    @Override
    public boolean supports(PaymentMethod method) {
        return method.isOnline(); // UPI, CARD, NET_BANKING, WALLET
    }

    @Override
    public PaymentResult initiate(Order order, PaymentMethod paymentMethod) {
        // Step 1: Create payment record (PENDING — awaiting gateway confirmation) and status history
        Payment createdPayment = paymentLifecycle.createPendingPayment(
                order,
                transactionIdGenerator.generate(),
                paymentMethod,
                paymentGateway.getName()
        );

        // Step 2: Create gateway order (HTTP call to Razorpay/Stripe, outside TX)
        GatewayOrder gatewayOrder;

        try {
            gatewayOrder = paymentGateway.createOrder(
                    order.getTotalAmount(),
                    createdPayment.getId().toString()
            );
        } catch (Exception e) {
            paymentLifecycle.markFailed(createdPayment.getId(), e.getMessage());
            throw e;
        }

        // Step 3: Persist in a short, isolated transaction
        Payment updatedPayment = paymentLifecycle.updateGatewayOrder(
                createdPayment.getId(),
                gatewayOrder
        );

        return new OnlinePaymentResult(
                updatedPayment.getId(),
                order.getId(),
                updatedPayment.getTransactionId(),
                paymentMethod,
                PaymentStatus.PENDING,
                order.getTotalAmount(),
                gatewayOrder.gatewayOrderId(),
                gatewayOrder.providerKeyId()
        );
    }
}
