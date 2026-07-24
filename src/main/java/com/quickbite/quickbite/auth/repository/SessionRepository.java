package com.quickbite.quickbite.auth.repository;

import com.quickbite.quickbite.auth.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.revokedAt = CURRENT_TIMESTAMP WHERE s.id = :id AND s.revokedAt IS NULL")
    int revokeSessionById(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.revokedAt = CURRENT_TIMESTAMP WHERE s.user.id = :userId AND s.revokedAt IS NULL")
    int revokeAllByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT s FROM Session s
            WHERE s.user.id = :userId AND s.revokedAt IS NULL AND s.expiresAt > CURRENT_TIMESTAMP
            ORDER BY s.createdAt ASC
            """)
    List<Session> findActiveByUserId(@Param("userId") UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.lastUsedAt = :now WHERE s.id = :id")
    int updateLastUsed(@Param("id") UUID id, @Param("now") Instant now);
}
