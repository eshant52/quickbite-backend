package com.quickbite.quickbite.payment.service.strategy;

import com.quickbite.quickbite.common.utils.TransactionIdGenerator;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.dto.StubOnlinePaymentResult;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;
import com.quickbite.quickbite.payment.model.PaymentStatusHistory;
import com.quickbite.quickbite.payment.service.PaymentLifecycleService;
import com.quickbite.quickbite.payment.service.gateway.PaymentGateway;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


/**
 * Stub strategy for all online payment methods (UPI, CARD, NET_BANKING, WALLET).
 *
 * <p><b>Dev profile only</b> — this bean is registered exclusively when
 * {@code spring.profiles.active=dev}. In all other profiles, {@link OnlinePaymentStrategy}
 * handles online payments via the real {@link PaymentGateway}.
 *
 * <p><b>Transaction contract (TX 2 of checkout):</b>
 * <ol>
 *   <li>Creates a {@link Payment} record (status = PENDING).</li>
 *   <li>Records a {@link PaymentStatusHistory} entry.</li>
 *   <li>Returns a {@link StubOnlinePaymentResult} with a fake {@code paymentUrl}
 *       that can be used for local development without real gateway credentials.</li>
 * </ol>
 */
@Profile("dev")
@Component
public class StubOnlinePaymentStrategy implements PaymentStrategy {

    private final PaymentLifecycleService paymentLifecycle;
    private final TransactionIdGenerator transactionIdGenerator;

    public StubOnlinePaymentStrategy(
            PaymentLifecycleService paymentLifecycle,
            TransactionIdGenerator transactionIdGenerator) {
        this.paymentLifecycle = paymentLifecycle;
        this.transactionIdGenerator = transactionIdGenerator;
    }

    @Override
    @Transactional
    public PaymentResult initiate(Order order, PaymentMethod paymentMethod) {

        Payment createdPayment = paymentLifecycle.createPendingPayment(
                order,
                transactionIdGenerator.generate("STUB"),
                paymentMethod,
                "STUB_GATEWAY"
        );

        String stubPaymentUrl = "https://stub-gateway.quickbite.local/pay?txn=" + createdPayment.getTransactionId();

        return new StubOnlinePaymentResult(
                createdPayment.getId(),
                order.getId(),
                createdPayment.getTransactionId(),
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
