package com.quickbite.quickbite.auth.repository;

import com.quickbite.quickbite.auth.model.RefreshTokenFamily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface RefreshTokenFamilyRepository extends JpaRepository<RefreshTokenFamily, UUID> {

    @Modifying
    @Transactional
    @Query("""
            UPDATE RefreshTokenFamily f
            SET f.revokedAt = CURRENT_TIMESTAMP, f.reuseDetectedAt = CURRENT_TIMESTAMP
            WHERE f.id = :familyId
            """)
    int revokeFamilyOnBreach(@Param("familyId") UUID familyId);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenFamily f SET f.revokedAt = CURRENT_TIMESTAMP WHERE f.session.id = :sessionId AND f.revokedAt IS NULL")
    int revokeFamiliesBySessionId(@Param("sessionId") UUID sessionId);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenFamily f SET f.revokedAt = CURRENT_TIMESTAMP WHERE f.session.user.id = :userId AND f.revokedAt IS NULL")
    int revokeFamiliesByUserId(@Param("userId") UUID userId);
}
