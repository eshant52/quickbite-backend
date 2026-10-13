package com.quickbite.quickbite.order.repository;

import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @Query("""
            SELECT o.id FROM Order o
            WHERE o.currentStatus IN :statuses
              AND o.createdAt < :cutoff
            ORDER BY o.createdAt ASC
            """)
    List<UUID> findStaleOrderIds(
            @Param("statuses") List<OrderStatus> statuses,
            @Param("cutoff") Instant cutoff,
            Limit limit
    );

    @Query("""
            SELECT o FROM Order o
            JOIN FETCH o.restaurant
            WHERE o.customer.id = :customerId
            ORDER BY o.id DESC
            """)
    List<Order> findByCustomerIdOrderByIdDesc(@Param("customerId") UUID customerId, Limit limit);

    @Query("""
            SELECT o FROM Order o
            JOIN FETCH o.restaurant
            WHERE o.customer.id = :customerId
              AND o.id < :cursor
            ORDER BY o.id DESC
            """)
    List<Order> findByCustomerIdAndIdLessThanOrderByIdDesc(
            @Param("customerId") UUID customerId,
            @Param("cursor") UUID cursor,
            Limit limit
    );

    default List<Order> findByCustomerWithCursor(
            UUID customerId,
            UUID cursor,
            Limit limit
    ) {
        return cursor == null
                ? findByCustomerIdOrderByIdDesc(customerId, limit)
                : findByCustomerIdAndIdLessThanOrderByIdDesc(customerId, cursor, limit);
    }

    @Query("""
            SELECT o FROM Order o
            JOIN FETCH o.restaurant
            WHERE o.restaurant.id = :restaurantId
              AND o.currentStatus = :status
            ORDER BY o.id DESC
            """)
    List<Order> findByRestaurantIdAndCurrentStatusOrderByIdDesc(
            @Param("restaurantId") UUID restaurantId,
            @Param("status") OrderStatus status,
            Limit limit
    );

    @Query("""
            SELECT o FROM Order o
            JOIN FETCH o.restaurant
            WHERE o.restaurant.id = :restaurantId
              AND o.currentStatus = :status
              AND o.id < :cursor
            ORDER BY o.id DESC
            """)
    List<Order> findByRestaurantIdAndCurrentStatusAndIdLessThanOrderByIdDesc(
            @Param("restaurantId") UUID restaurantId,
            @Param("status") OrderStatus status,
            @Param("cursor") UUID cursor,
            Limit limit
    );

    @Query("""
            SELECT o FROM Order o
            JOIN FETCH o.restaurant
            WHERE o.restaurant.id = :restaurantId
            ORDER BY o.id DESC
            """)
    List<Order> findByRestaurantIdOrderByIdDesc(
            @Param("restaurantId") UUID restaurantId,
            Limit limit
    );

    @Query("""
            SELECT o FROM Order o
            JOIN FETCH o.restaurant
            WHERE o.restaurant.id = :restaurantId
              AND o.id < :cursor
            ORDER BY o.id DESC
            """)
    List<Order> findByRestaurantIdAndIdLessThanOrderByIdDesc(
            @Param("restaurantId") UUID restaurantId,
            @Param("cursor") UUID cursor,
            Limit limit
    );

    default List<Order> findByRestaurantWithCursor(
            UUID restaurantId,
            OrderStatus status,
            UUID cursor,
            Limit limit
    ) {
        if (status != null) {
            return cursor == null
                    ? findByRestaurantIdAndCurrentStatusOrderByIdDesc(restaurantId, status, limit)
                    : findByRestaurantIdAndCurrentStatusAndIdLessThanOrderByIdDesc(restaurantId, status, cursor, limit);
        } else {
            return cursor == null
                    ? findByRestaurantIdOrderByIdDesc(restaurantId, limit)
                    : findByRestaurantIdAndIdLessThanOrderByIdDesc(restaurantId, cursor, limit);
        }
    }

    Optional<Order> findByIdAndCustomerId(UUID id, UUID customerId);

    @EntityGraph(attributePaths = {"restaurant", "deliveryAgent", "deliveryAgent.user", "items"})
    Optional<Order> findDetailedByIdAndCustomerId(UUID id, UUID customerId);

    Optional<Order> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    @EntityGraph(attributePaths = {"restaurant", "deliveryAgent", "deliveryAgent.user", "items"})
    Optional<Order> findDetailedByIdAndRestaurantId(UUID id, UUID restaurantId);

    @Query("""
            SELECT o.id FROM Order o
            WHERE o.currentStatus = :status
              AND o.restaurantAcceptanceDeadline < :now
            ORDER BY o.restaurantAcceptanceDeadline ASC
            """)
    List<UUID> findUnacceptedOrderIds(
            @Param("status") OrderStatus status,
            @Param("now") Instant now,
            Limit limit
    );

    Optional<Order> findByIdAndCurrentStatusNotIn(UUID id, List<OrderStatus> statuses);
}
