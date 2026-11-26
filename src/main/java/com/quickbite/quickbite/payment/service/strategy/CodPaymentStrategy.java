package com.quickbite.quickbite.payment.service.strategy;

import com.quickbite.quickbite.common.utils.TransactionIdGenerator;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.payment.dto.CodPaymentResult;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.service.PaymentLifecycleService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment strategy for Cash on Delivery orders.
 *
 * <p><b>Transaction contract (TX 2 of checkout):</b>
 * <ol>
 *   <li>Creates the {@link Payment} entity (status = {@code PENDING}) and initial status history.</li>
 *   <li>{@link PaymentLifecycleService#createPendingPayment} publishes {@code CodPaymentInitiatedEvent}
 *       ({@code BEFORE_COMMIT}) so the order domain transitions the order to {@code PLACED} and
 *       publishes {@code OrderPlacedEvent}.</li>
 *   <li>The payment remains {@code PENDING} until doorstep delivery collection transitions it
 *       to {@code SUCCESS}.</li>
 * </ol>
 */
@Component
public class CodPaymentStrategy implements PaymentStrategy {

    private final PaymentLifecycleService paymentLifecycle;
    private final TransactionIdGenerator transactionIdGenerator;

    public CodPaymentStrategy(
            PaymentLifecycleService paymentLifecycle,
            TransactionIdGenerator transactionIdGenerator) {
        this.paymentLifecycle = paymentLifecycle;
        this.transactionIdGenerator = transactionIdGenerator;
    }

    @Override
    @Transactional
    public PaymentResult initiate(Order order, PaymentMethod paymentMethod) {
        Payment payment = paymentLifecycle.createPendingPayment(
                order,
                transactionIdGenerator.generate("COD"),
                paymentMethod
        );

        return new CodPaymentResult(
                payment.getId(),
                order.getId(),
                payment.getTransactionId(),
                payment.getAmount()
        );
    }

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return PaymentMethod.COD == paymentMethod;
    }
}
