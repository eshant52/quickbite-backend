package com.quickbite.quickbite.menu.repository;

import com.quickbite.quickbite.menu.model.CuisineRequest;
import com.quickbite.quickbite.menu.model.CuisineStatus;
import com.quickbite.quickbite.user.model.User;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CuisineRequestRepository extends JpaRepository<CuisineRequest, UUID> {

    boolean existsByNameIgnoreCaseAndStatus(String name, CuisineStatus status);

    Page<CuisineRequest> findByRequestedBy(User requestedBy, Pageable pageable);

    @Query("""
    SELECT r FROM CuisineRequest r
    WHERE (:cursor IS NULL OR r.id > :cursor)
        AND (:status IS NULL OR r.status = :status)
    ORDER BY r.id ASC
    """)
    List<CuisineRequest> findWithCursor(
            @Param("cursor") UUID cursor,
            @Param("status") CuisineStatus status,
            Limit limit
    );
}
