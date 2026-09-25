package com.quickbite.quickbite.delivery.repository;

import com.quickbite.quickbite.delivery.model.DeliveryOffer;
import com.quickbite.quickbite.delivery.model.DeliveryOfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface DeliveryOfferRepository extends JpaRepository<DeliveryOffer, UUID> {

    Optional<DeliveryOffer> findByOrderIdAndStatus(UUID orderId, DeliveryOfferStatus status);

    Optional<DeliveryOffer> findByOrderIdAndAgentIdAndStatus(UUID orderId, UUID agentId, DeliveryOfferStatus status);

    Optional<DeliveryOffer> findByOrderIdAndAgentIdAndStatusIn(
            UUID orderId,
            UUID agentId,
            List<DeliveryOfferStatus> statuses
    );

    @Query("SELECT o.agent.id FROM DeliveryOffer o WHERE o.orderId = :orderId")
    Set<UUID> findOfferedAgentIdsByOrderId(@Param("orderId") UUID orderId);

    @Query("""
            SELECT o FROM DeliveryOffer o
            WHERE o.status = :status
              AND o.expiresAt <= :now
            """)
    List<DeliveryOffer> findExpiredOffers(
            @Param("status") DeliveryOfferStatus status,
            @Param("now") Instant now
    );
}
