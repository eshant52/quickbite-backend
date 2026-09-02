package com.quickbite.quickbite.delivery.repository;

import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.onboarding.model.deliveryagent.DeliveryAgentVerificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryAgentVerificationHistoryRepository extends JpaRepository<DeliveryAgentVerificationHistory, UUID> {
    List<DeliveryAgentVerificationHistory> findByDeliveryAgentOrderByCreatedAtDesc(DeliveryAgent deliveryAgent);
}
