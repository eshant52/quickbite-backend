package com.quickbite.quickbite.delivery.repository;

import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.model.DeliveryAgentDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryAgentDocumentRepository extends JpaRepository<DeliveryAgentDocument, UUID> {
    List<DeliveryAgentDocument> findByDeliveryAgent(DeliveryAgent deliveryAgent);
}
