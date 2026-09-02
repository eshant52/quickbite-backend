package com.quickbite.quickbite.onboarding.repository;

import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.model.deliveryagent.DeliveryAgentApplication;
import com.quickbite.quickbite.user.model.User;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryAgentApplicationRepository extends JpaRepository<DeliveryAgentApplication, UUID> {

    Optional<DeliveryAgentApplication> findByAgentAndStatusIn(User agent, List<ApplicationStatus> statuses);

    Optional<DeliveryAgentApplication> findByIdAndAgent(UUID id, User agent);

    boolean existsByAgentAndStatusIn(User agent, List<ApplicationStatus> statuses);

    @Query("""
            SELECT da FROM DeliveryAgentApplication da
            WHERE (:status IS NULL OR da.status = :status)
              AND (:cursor IS NULL OR da.id < :cursor)
            ORDER BY da.id DESC
            """)
    List<DeliveryAgentApplication> findWithCursor(
            @Param("status") ApplicationStatus status,
            @Param("cursor") UUID cursor,
            Limit limit
    );
}
