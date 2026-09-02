package com.quickbite.quickbite.delivery.repository;

import com.quickbite.quickbite.delivery.model.DeliveryAgent;
import com.quickbite.quickbite.delivery.model.DeliveryAgentVerificationStatus;
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
public interface DeliveryAgentRepository extends JpaRepository<DeliveryAgent, UUID> {

    Optional<DeliveryAgent> findByUser(User user);

    Optional<DeliveryAgent> findByIdAndUser(UUID id, User user);

    boolean existsByUser(User user);

    @Query(value = """
            SELECT da.* FROM delivery_agents da
            WHERE da.is_available = true
              AND da.current_status = 'APPROVED'
              AND da.last_location IS NOT NULL
            ORDER BY ST_Distance(da.last_location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)) ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<DeliveryAgent> findNearestAvailableAgents(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("limit") int limit
    );

    @Query("""
            SELECT da FROM DeliveryAgent da
            WHERE (:status IS NULL OR da.currentStatus = :status)
              AND (:cursor IS NULL OR da.id < :cursor)
            ORDER BY da.id DESC
            """)
    List<DeliveryAgent> findWithCursor(
            @Param("status") DeliveryAgentVerificationStatus status,
            @Param("cursor") UUID cursor,
            Limit limit
    );
}
