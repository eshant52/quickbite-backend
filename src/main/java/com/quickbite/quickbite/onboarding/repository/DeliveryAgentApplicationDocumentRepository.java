package com.quickbite.quickbite.onboarding.repository;

import com.quickbite.quickbite.delivery.model.DeliveryAgentDocumentType;
import com.quickbite.quickbite.onboarding.model.deliveryagent.DeliveryAgentApplication;
import com.quickbite.quickbite.onboarding.model.deliveryagent.DeliveryAgentApplicationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryAgentApplicationDocumentRepository extends JpaRepository<DeliveryAgentApplicationDocument, UUID> {
    List<DeliveryAgentApplicationDocument> findByApplication(DeliveryAgentApplication application);
    Optional<DeliveryAgentApplicationDocument> findByApplicationAndType(DeliveryAgentApplication application, DeliveryAgentDocumentType type);
    boolean existsByApplicationAndType(DeliveryAgentApplication application, DeliveryAgentDocumentType type);
    void deleteByApplicationAndType(DeliveryAgentApplication application, DeliveryAgentDocumentType type);
}
