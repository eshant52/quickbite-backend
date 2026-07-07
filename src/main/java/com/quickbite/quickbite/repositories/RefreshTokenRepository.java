package com.quickbite.quickbite.repositories;

import com.quickbite.quickbite.models.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    /**
     * Atomic validate-and-revoke in a single statement.
     * If this return 1, the caller "won" the race and may issue a new token.
     * If this return 0, the token was already revoked/rotated by a concurrent
     * request (or never existed) - caller must reject immediately, no separate
     * read-then-write window exists for two requests to both succeed.
     */
    @Modifying
    @Transactional
    @Query("""
            UPDATE RefreshToken
            SET revoked = true
            WHERE tokenHash = :tokenHash and revoked = false AND expiresAt> :now
            """)
    int atomicRevokeIfActive(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    Optional<RefreshToken> findRefreshTokenByTokenHash(String hash);

    List<RefreshToken> findRefreshTokenByFamilyIdOrderByCreatedAtDesc(UUID familyId);

    List<RefreshToken> findRefreshTokensByUserIdAndRevokedIsFalseOrderByCreatedAtAsc(UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken SET revoked = true WHERE familyId = :familyId AND revoked = false")
    int revokeFamily(@Param("familyId") UUID familyId);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken SET revoked = true WHERE user.id = :userId AND revoked = false")
    int revokeAllForUser(@Param("userId") UUID userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken WHERE (revoked = true OR expiresAt < :now) AND createdAt < :cutoff")
    int purgeStaleToken(@Param("now") Instant now, @Param("cutoff") Instant cutoff);
}
