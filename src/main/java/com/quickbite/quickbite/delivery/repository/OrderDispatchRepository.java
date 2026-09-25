package com.quickbite.quickbite.delivery.repository;

import com.quickbite.quickbite.delivery.model.DeliveryDispatchStatus;
import com.quickbite.quickbite.delivery.model.OrderDispatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderDispatchRepository extends JpaRepository<OrderDispatch, UUID> {

    Optional<OrderDispatch> findByOrderId(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM OrderDispatch d WHERE d.orderId = :orderId")
    Optional<OrderDispatch> findByOrderIdForUpdate(@Param("orderId") UUID orderId);

    @Query(value = """
            SELECT d.order_id FROM order_dispatches d
            WHERE d.status = 'FINDING_AGENT'
              AND d.next_attempt_at <= :now
            ORDER BY d.next_attempt_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<UUID> findDueDispatches(
            @Param("now") Instant now,
            @Param("limit") int limit
    );
}
