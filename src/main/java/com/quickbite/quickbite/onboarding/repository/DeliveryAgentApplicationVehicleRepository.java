package com.quickbite.quickbite.onboarding.repository;

import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.model.deliveryagent.DeliveryAgentApplication;
import com.quickbite.quickbite.onboarding.model.vehicle.VehicleApplication;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryAgentApplicationVehicleRepository extends JpaRepository<VehicleApplication, UUID> {

    Optional<VehicleApplication> findByApplication(DeliveryAgentApplication application);

    List<VehicleApplication> findByDeliveryAgent(DeliveryAgent deliveryAgent);

    Optional<VehicleApplication> findByIdAndDeliveryAgent(UUID id, DeliveryAgent deliveryAgent);

    Optional<VehicleApplication> findByVinNumber(String vinNumber);

    boolean existsByDeliveryAgentAndStatusIn(DeliveryAgent deliveryAgent, List<ApplicationStatus> statuses);

    /**
     * Ownership-aware lookup that works for both flows:
     * - Flow 1 (Onboarding):   application.agent.id = :userId
     * - Flow 2 (Standalone):   deliveryAgent.user.id = :userId
     */
    @Query("""
            SELECT dav FROM VehicleApplication dav
            WHERE dav.id = :id
              AND (
                (dav.application IS NOT NULL AND dav.application.agent.id = :userId)
                OR
                (dav.deliveryAgent IS NOT NULL AND dav.deliveryAgent.user.id = :userId)
              )
            """)
    Optional<VehicleApplication> findByIdAndOwnerUserId(
            @Param("id") UUID id,
            @Param("userId") UUID userId
    );

    @Query("""
            SELECT dav FROM VehicleApplication dav
            WHERE dav.deliveryAgent IS NOT NULL
              AND (:status IS NULL OR dav.status = :status)
              AND (:cursor IS NULL OR dav.id < :cursor)
            ORDER BY dav.id DESC
            """)
    List<VehicleApplication> findStandaloneWithCursor(
            @Param("status") ApplicationStatus status,
            @Param("cursor") UUID cursor,
            Limit limit
    );
}
