package com.quickbite.quickbite.payment.repository;

import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(UUID orderId);
    Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(UUID orderId);
    Optional<Payment> findTopByOrderIdAndCurrentStatus(UUID orderId, PaymentStatus status);
    List<Payment> findByOrderIdAndCurrentStatus(UUID orderId, PaymentStatus status);
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);
    Optional<Payment> findByGatewayOrderIdAndGatewayPaymentIdAndCurrentStatus(String gatewayOrderId, String gatewayPaymentId, PaymentStatus status);

    @Query("""
    SELECT p FROM Payment p
    WHERE p.order.id = :orderId
        AND p.order.customer.id = :customerId
    ORDER BY p.createdAt ASC
    """)
    List<Payment> findAllByOrderIdAndCustomerId(
            @Param("orderId") UUID orderId,
            @Param("customerId") UUID customerId
    );

    @Query("""
    SELECT p FROM Payment p
    WHERE p.order.id = :orderId
    ORDER BY p.createdAt ASC
    """)
    List<Payment> findAllByOrderIdOrderByCreatedAtAsc(@Param("orderId") UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.gatewayOrderId = :gatewayOrderId")
    Optional<Payment> findByGatewayOrderIdForUpdate(@Param("gatewayOrderId") String gatewayOrderId);

    @Modifying
    @Query("""
    UPDATE Payment p
    SET p.currentStatus = :status
    WHERE p.id = :paymentId
        AND p.currentStatus != :status
    """)
    int updateCurrentStatus(@Param("paymentId") UUID paymentId, @Param("status") PaymentStatus status);

    @Query("""
    SELECT p FROM Payment p
    WHERE p.order.id = :orderId
        AND (p.gatewayOrderId IS NOT NULL OR p.paymentMethod = PaymentMethod.COD)
        AND p.currentStatus NOT IN :excludedStatuses
    ORDER BY p.createdAt ASC
    """)
    List<Payment> findAttemptsForReconciliation(
            @Param("orderId") UUID orderId,
            @Param("excludedStatuses") Collection<PaymentStatus> excludedStatuses
    );
}
