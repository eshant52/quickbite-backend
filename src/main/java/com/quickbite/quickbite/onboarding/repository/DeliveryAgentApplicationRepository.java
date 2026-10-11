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

    List<DeliveryAgentApplication> findByStatusOrderByIdDesc(ApplicationStatus status, Limit limit);

    List<DeliveryAgentApplication> findByStatusAndIdLessThanOrderByIdDesc(ApplicationStatus status, UUID cursor, Limit limit);

    List<DeliveryAgentApplication> findAllByOrderByIdDesc(Limit limit);

    List<DeliveryAgentApplication> findByIdLessThanOrderByIdDesc(UUID cursor, Limit limit);

    default List<DeliveryAgentApplication> findWithCursor(
            ApplicationStatus status,
            UUID cursor,
            Limit limit
    ) {
        if (status != null) {
            return cursor == null
                    ? findByStatusOrderByIdDesc(status, limit)
                    : findByStatusAndIdLessThanOrderByIdDesc(status, cursor, limit);
        } else {
            return cursor == null
                    ? findAllByOrderByIdDesc(limit)
                    : findByIdLessThanOrderByIdDesc(cursor, limit);
        }
    }
}
