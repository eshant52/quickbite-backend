package com.quickbite.quickbite.payment.service.strategy;

import com.quickbite.quickbite.cart.repository.CartRepository;
import com.quickbite.quickbite.common.event.order.OrderPlacedEvent;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.payment.dto.CodPaymentResult;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;
import com.quickbite.quickbite.payment.model.PaymentStatusHistory;
import com.quickbite.quickbite.payment.repository.PaymentRepository;
import com.quickbite.quickbite.payment.repository.PaymentStatusHistoryRepository;
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

    private final PaymentRepository paymentRepository;
    private final PaymentStatusHistoryRepository paymentStatusHistoryRepository;
    private final CartRepository cartRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CodPaymentStrategy(
            PaymentRepository paymentRepository,
            PaymentStatusHistoryRepository paymentStatusHistoryRepository,
            CartRepository cartRepository,
            ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.paymentStatusHistoryRepository = paymentStatusHistoryRepository;
        this.cartRepository = cartRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public PaymentResult initiate(Order order, PaymentMethod paymentMethod) {

        // 1. Persist Payment record
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(PaymentMethod.COD);
        payment.setTransactionId("COD-" + order.getId().toString().substring(0, 8).toUpperCase());
        payment.setAmount(order.getTotalAmount());
        payment.setCurrentStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        // 2. Persist payment status history
        PaymentStatusHistory paymentHistory = new PaymentStatusHistory();
        paymentHistory.setPayment(payment);
        paymentHistory.setStatus(PaymentStatus.PENDING);
        paymentStatusHistoryRepository.save(paymentHistory);

        // 3. Clear cart — safe for COD (order is committed, no gateway risk)
        cartRepository.findByCustomer(order.getCustomer())
                .ifPresent(cartRepository::delete);

        // 4. Register OrderPlacedEvent to be published AFTER this transaction commits.
        //    OrderKafkaEventPublisher picks this up via @TransactionalEventListener(AFTER_COMMIT),
        //    ensuring Kafka consumers always find committed order data.
        eventPublisher.publishEvent(new OrderPlacedEvent(
                order.getId(),
                order.getCustomer().getId(),
                order.getCustomer().getName(),
                order.getCustomer().getEmail(),
                order.getRestaurant().getId(),
                order.getRestaurant().getName(),
                order.getTotalAmount(),
                order.getCreatedAt()
        ));

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
