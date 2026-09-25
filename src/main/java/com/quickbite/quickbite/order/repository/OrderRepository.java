package com.quickbite.quickbite.order.repository;

import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("""
            SELECT o FROM Order o
            WHERE o.currentStatus = :status
              AND o.createdAt < :cutoff
            ORDER BY o.createdAt ASC
            """)
    List<Order> findByCurrentStatusAndCreatedAtBeforeForUpdateSkipLocked(
            @Param("status") OrderStatus status,
            @Param("cutoff") Instant cutoff,
            Limit limit
    );

    @Query("""
            SELECT o FROM Order o
            WHERE o.customer.id = :customerId
              AND (:cursor IS NULL OR o.id < :cursor)
            ORDER BY o.id DESC
            """)
    List<Order> findByCustomerWithCursor(
            @Param("customerId") UUID customerId,
            @Param("cursor") UUID cursor,
            Limit limit
    );

    @Query("""
            SELECT o FROM Order o
            WHERE o.restaurant.id = :restaurantId
              AND (:status IS NULL OR o.currentStatus = :status)
              AND (:cursor IS NULL OR o.id < :cursor)
            ORDER BY o.id DESC
            """)
    List<Order> findByRestaurantWithCursor(
            @Param("restaurantId") UUID restaurantId,
            @Param("status") OrderStatus status,
            @Param("cursor") UUID cursor,
            Limit limit
    );

    Optional<Order> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<Order> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("""
            SELECT o FROM Order o
            WHERE o.currentStatus = :status
              AND o.restaurantAcceptanceDeadline < :now
            ORDER BY o.restaurantAcceptanceDeadline ASC
            """)
    List<Order> findByCurrentStatusAndRestaurantAcceptanceDeadlineBeforeForUpdateSkipLocked(
            @Param("status") OrderStatus status,
            @Param("now") Instant now,
            Limit limit
    );

    Optional<Order> findByIdAndCurrentStatusNotIn(UUID id, List<OrderStatus> statuses);
}
