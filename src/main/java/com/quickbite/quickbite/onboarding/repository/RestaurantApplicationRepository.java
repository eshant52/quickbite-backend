package com.quickbite.quickbite.onboarding.repository;

import com.quickbite.quickbite.onboarding.model.ApplicationStatus;
import com.quickbite.quickbite.onboarding.model.RestaurantApplication;
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
public interface RestaurantApplicationRepository extends JpaRepository<RestaurantApplication, UUID> {

    Optional<RestaurantApplication> findByOwnerAndStatusIn(User owner, List<ApplicationStatus> statuses);

    Optional<RestaurantApplication> findByIdAndOwner(UUID id, User owner);

    /**
     * Cursor-based admin listing by status.
     *
     * Fetch {@code limit} applications with {@code id > cursor} (or all when cursor is null).
     * Callers pass {@code Limit.of(requestedSize + 1)} to enable hasMore detection.
     */
    @Query("""
            SELECT a FROM RestaurantApplication a
            WHERE a.status = :status
              AND (:cursor IS NULL OR a.id > :cursor)
            ORDER BY a.id ASC
            """)
    List<RestaurantApplication> findByStatusWithCursor(
            @Param("status") ApplicationStatus status,
            @Param("cursor") UUID cursor,
            Limit limit);

    boolean existsByOwnerAndStatusIn(User owner, List<ApplicationStatus> statuses);
}
