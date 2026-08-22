package com.quickbite.quickbite.menu.repository;

import com.quickbite.quickbite.menu.model.Cuisine;
import com.quickbite.quickbite.menu.model.CuisineStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CuisineRepository extends JpaRepository<Cuisine, UUID> {
    boolean existsByNameIgnoreCase(String name);
    List<Cuisine> findByStatus(CuisineStatus status);

    @Query("""
    SELECT c FROM Cuisine c
    WHERE (:cursor IS NULL OR c.id > :cursor)
        AND (:status IS NULL OR c.status = :status)
    ORDER BY c.id ASC
    """)
    List<Cuisine> findWithCursor(
            @Param("cursor") UUID cursor,
            @Param("status") CuisineStatus status,
            Limit limit
    );
}
