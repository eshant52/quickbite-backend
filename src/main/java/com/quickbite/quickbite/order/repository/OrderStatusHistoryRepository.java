package com.quickbite.quickbite.order.repository;

import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {
    List<OrderStatusHistory> findByOrderOrderByCreatedAtAsc(Order order);
}
