package com.quickbite.quickbite.allotment.repository;

import com.quickbite.quickbite.allotment.model.AdminAllotment;
import com.quickbite.quickbite.allotment.model.AllotmentStatus;
import com.quickbite.quickbite.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminAllotmentRepository extends JpaRepository<AdminAllotment, UUID> {

    List<AdminAllotment> findByReferenceIdAndStatus(UUID referenceId, AllotmentStatus status);

    List<AdminAllotment> findByAdminAndStatus(User admin, AllotmentStatus status);

    Optional<AdminAllotment> findByIdAndAdmin(UUID id, User admin);

    boolean existsByReferenceId(UUID referenceId);

    @Query("SELECT a.admin.id, COUNT(a) FROM AdminAllotment a WHERE a.status = 'PENDING' GROUP BY a.admin.id")
    List<Object[]> countPendingTasksPerAdmin();

    @Modifying
    @Query("""
        UPDATE AdminAllotment a
        SET a.status = 'DECLINED', a.respondedAt = :now
        WHERE a.referenceId = :referenceId
          AND a.status = 'PENDING'
          AND a.id <> :acceptedAllotmentId
        """)
    int declineOthers(
            @Param("referenceId") UUID referenceId,
            @Param("acceptedAllotmentId") UUID acceptedAllotmentId,
            @Param("now") Instant now
    );
}
