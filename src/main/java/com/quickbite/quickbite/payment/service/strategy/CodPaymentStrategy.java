package com.quickbite.quickbite.payment.service.strategy;

import com.quickbite.quickbite.common.event.order.OrderPlacedEvent;
import com.quickbite.quickbite.common.utils.TransactionIdGenerator;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.payment.dto.CodPaymentResult;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatusHistory;
import com.quickbite.quickbite.payment.service.PaymentLifecycleService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment strategy for Cash on Delivery orders.
 *
 * <p><b>Transaction contract (TX 2 of checkout):</b>
 * <ol>
 *   <li>Creates the {@link Payment} entity (status = PENDING — confirmed on delivery).</li>
 *   <li>Records a {@link PaymentStatusHistory} entry.</li>
 *   <li>Clears the customer's cart — it is safe to do so because the order is already
 *       persisted and no gateway failure can occur for COD.</li>
 *   <li>Publishes {@link OrderPlacedEvent} via Spring's {@link ApplicationEventPublisher}.
 *       Because this is called inside a {@code @Transactional} method, the event is
 *       registered but only dispatched to {@code OrderKafkaEventPublisher} <em>after
 *       this transaction commits</em> (AFTER_COMMIT phase), guaranteeing Kafka consumers
 *       will always find the committed order data when they query.</li>
 * </ol>
 *
 * <p>Note: the Order was already set to {@code PLACED} and its initial
 * {@code OrderStatusHistory} was written by {@code OrderCreationServiceImpl} (TX 1).
 * This strategy does not touch the Order entity again.
 */
@Component
public class CodPaymentStrategy implements PaymentStrategy {

    private final PaymentLifecycleService paymentLifecycle;
    private final TransactionIdGenerator transactionIdGenerator;
    private final ApplicationEventPublisher eventPublisher;

    public CodPaymentStrategy(
            PaymentLifecycleService paymentLifecycle,
            TransactionIdGenerator transactionIdGenerator,
            ApplicationEventPublisher eventPublisher) {
        this.paymentLifecycle = paymentLifecycle;
        this.transactionIdGenerator = transactionIdGenerator;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public PaymentResult initiate(Order order, PaymentMethod paymentMethod) {
        // 1. Create the payment with status pending
        Payment payment = paymentLifecycle.createPendingPayment(
                order,
                transactionIdGenerator.generate("COD"),
                paymentMethod
        );

        paymentLifecycle.processCodPaymentSuccess(payment.getId());

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
