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
            LEFT JOIN dav.application daa
            LEFT JOIN daa.agent u1
            LEFT JOIN dav.deliveryAgent da
            LEFT JOIN da.user u2
            WHERE dav.id = :id
              AND (u1.id = :userId OR u2.id = :userId)
            """)
    Optional<VehicleApplication> findByIdAndOwnerUserId(
            @Param("id") UUID id,
            @Param("userId") UUID userId
    );

    List<VehicleApplication> findByDeliveryAgentIsNotNullAndStatusOrderByIdDesc(
            ApplicationStatus status,
            Limit limit
    );

    List<VehicleApplication> findByDeliveryAgentIsNotNullAndStatusAndIdLessThanOrderByIdDesc(
            ApplicationStatus status,
            UUID cursor,
            Limit limit
    );

    List<VehicleApplication> findByDeliveryAgentIsNotNullOrderByIdDesc(
            Limit limit
    );

    List<VehicleApplication> findByDeliveryAgentIsNotNullAndIdLessThanOrderByIdDesc(
            UUID cursor,
            Limit limit
    );

    default List<VehicleApplication> findStandaloneWithCursor(
            ApplicationStatus status,
            UUID cursor,
            Limit limit
    ) {
        if (status != null) {
            return cursor == null
                    ? findByDeliveryAgentIsNotNullAndStatusOrderByIdDesc(status, limit)
                    : findByDeliveryAgentIsNotNullAndStatusAndIdLessThanOrderByIdDesc(status, cursor, limit);
        } else {
            return cursor == null
                    ? findByDeliveryAgentIsNotNullOrderByIdDesc(limit)
                    : findByDeliveryAgentIsNotNullAndIdLessThanOrderByIdDesc(cursor, limit);
        }
    }
}
