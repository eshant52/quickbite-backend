package com.quickbite.quickbite.auth.repository;

import com.quickbite.quickbite.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Query("""
            SELECT rt FROM RefreshToken rt
            JOIN FETCH rt.family f
            JOIN FETCH f.session s
            JOIN FETCH s.user u
            WHERE rt.tokenHash = :hash
            """)
    Optional<RefreshToken> findRefreshTokenWithFamilyAndSessionByTokenHash(@Param("hash") String hash);

    /**
     * Atomically marks a refresh token as used if it hasn't been used yet.
     * Returns 1 if marked successfully (safe path), 0 if it was already used (token reuse breach!).
     */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.usedAt = :now WHERE rt.id = :id AND rt.usedAt IS NULL")
    int markTokenUsed(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken WHERE (revokedAt IS NOT NULL OR expiresAt < :now) AND createdAt < :cutoff")
    int purgeStaleToken(@Param("now") Instant now, @Param("cutoff") Instant cutoff);
}
