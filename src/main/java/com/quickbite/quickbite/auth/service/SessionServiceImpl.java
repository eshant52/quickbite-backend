package com.quickbite.quickbite.auth.service;

import com.quickbite.quickbite.auth.dto.DeviceInfo;
import com.quickbite.quickbite.auth.dto.IssuedToken;
import com.quickbite.quickbite.auth.dto.SessionResponse;
import com.quickbite.quickbite.auth.exception.AuthenticationException;
import com.quickbite.quickbite.auth.model.RefreshToken;
import com.quickbite.quickbite.auth.model.RefreshTokenFamily;
import com.quickbite.quickbite.auth.model.Session;
import com.quickbite.quickbite.auth.repository.RefreshTokenRepository;
import com.quickbite.quickbite.auth.util.TokenUtils;
import com.quickbite.quickbite.common.config.property.AuthProperties;
import com.quickbite.quickbite.user.model.User;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SessionServiceImpl implements SessionService {

    private static final String LOCK_PREFIX = "quickbite:session-lock:";
    private static final String REFRESH_LOCK_PREFIX = "quickbite:refresh-lock:";
    private static final Duration ROTATION_GRACE_PERIOD = Duration.ofSeconds(2);

    private final SessionPersistenceService sessionPersistenceService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionStoreService sessionStoreService;
    private final AuthProperties authProperties;
    private final RedissonClient redissonClient;

    public SessionServiceImpl(
            SessionPersistenceService sessionPersistenceService,
            RefreshTokenRepository refreshTokenRepository,
            SessionStoreService sessionStoreService,
            AuthProperties authProperties,
            RedissonClient redissonClient) {
        this.sessionPersistenceService = sessionPersistenceService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.sessionStoreService = sessionStoreService;
        this.authProperties = authProperties;
        this.redissonClient = redissonClient;
    }

    @Override
    public IssuedToken createNewSession(User user, DeviceInfo deviceInfo) {
        UUID userId = user.getId();
        RLock lock = redissonClient.getLock(LOCK_PREFIX + userId);
        boolean locked;
        try {
            locked = lock.tryLock(0, 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthenticationException("Session creation interrupted. Please try again.");
        }

        if (!locked) {
            throw new AuthenticationException("Session creation is already in progress. Please try again.");
        }

        try {
            Instant expiresAt = Instant.now().plus(authProperties.jwt().refreshTokenExpiry());
            String rawToken = TokenUtils.generateOpaqueToken();

            // 1. Transactional persistence: commits to PostgreSQL before returning
            Session session = sessionPersistenceService.persistNewSession(user, deviceInfo, expiresAt, rawToken);

            return new IssuedToken(rawToken, session.getId(), user.getId());
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public IssuedToken validateAndRotate(String rawToken) {
        String hash = TokenUtils.sha256(rawToken);

        // 1. Fast path: check grace cache before acquiring lock
        Optional<String> fastGraceToken = sessionStoreService.getRotatedTokenGrace(hash);
        if (fastGraceToken.isPresent()) {
            return resolveIssuedTokenForCachedHash(hash, fastGraceToken.get());
        }

        // 2. Lock on the token hash so concurrent requests (e.g. 5ms apart) queue up safely
        RLock lock = redissonClient.getLock(REFRESH_LOCK_PREFIX + hash);
        boolean locked;
        try {
            locked = lock.tryLock(3, 5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthenticationException("Token rotation interrupted. Please try again.");
        }

        if (!locked) {
            throw new AuthenticationException("Token rotation already in progress. Please try again.");
        }

        try {
            // 3. Double-checked locking: Re-check grace cache after acquiring lock
            // If another concurrent request just completed rotation, it will have populated the cache!
            Optional<String> cachedToken = sessionStoreService.getRotatedTokenGrace(hash);
            if (cachedToken.isPresent()) {
                return resolveIssuedTokenForCachedHash(hash, cachedToken.get());
            }

            // 4. Normal rotation logic
            RefreshToken existing = refreshTokenRepository.findRefreshTokenWithFamilyAndSessionByTokenHash(hash)
                    .orElseThrow(() -> new AuthenticationException("Invalid or expired refresh token"));

            if (existing.getExpiresAt().isBefore(Instant.now())) {
                throw new AuthenticationException("Refresh token has expired");
            }

            RefreshTokenFamily family = existing.getFamily();
            if (family.getRevokedAt() != null) {
                throw new AuthenticationException("Session has been revoked");
            }

            Session session = family.getSession();
            if (session.getRevokedAt() != null) {
                throw new AuthenticationException("Session has been revoked");
            }

            if (session.getExpiresAt().isBefore(Instant.now())) {
                throw new AuthenticationException("Session has expired");
            }

            if (!session.getUser().isActive()) {
                throw new AuthenticationException("Account is deactivated");
            }

            // Try marking token used
            boolean marked = sessionPersistenceService.markTokenUsed(existing.getId(), Instant.now());

            if (!marked) {
                // BREACH DETECTED! Token reuse outside grace period
                // Revoke in an independent committed transaction (REQUIRES_NEW)
                sessionPersistenceService.revokeBreachedFamily(family.getId(), session.getId(), session.getUser().getId());

                throw new AuthenticationException("Refresh token reuse detected. Session has been revoked. Please login again.");
            }

            // SAFE ROTATION PATH
            String newRawToken = TokenUtils.generateOpaqueToken();
            sessionPersistenceService.saveRotatedToken(
                    family,
                    session,
                    newRawToken,
                    existing.getGeneration() + 1,
                    existing.getExpiresAt()
            );

            // Cache for 2-second grace window for concurrent requests/retries
            sessionStoreService.cacheRotatedTokenGrace(hash, newRawToken, ROTATION_GRACE_PERIOD);

            return new IssuedToken(newRawToken, session.getId(), session.getUser().getId());
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public void revokeSession(UUID userId, UUID sessionId) {
        sessionPersistenceService.revokeSession(userId, sessionId);
    }

    @Override
    public void revokeAllSessions(UUID userId) {
        sessionPersistenceService.revokeAllSessions(userId);
    }

    @Override
    public List<SessionResponse> listActiveSessionsForUser(UUID userId) {
        List<Session> sessions = sessionPersistenceService.findActiveSessionsByUserId(userId);
        return sessions.stream()
                .map(SessionResponse::from)
                .toList();
    }

    private IssuedToken resolveIssuedTokenForCachedHash(String hash, String cachedRawToken) {
        RefreshToken token = refreshTokenRepository.findRefreshTokenWithFamilyAndSessionByTokenHash(hash)
                .orElseThrow(() -> new AuthenticationException("Invalid or expired refresh token"));
        Session session = token.getFamily().getSession();
        return new IssuedToken(cachedRawToken, session.getId(), session.getUser().getId());
    }
}
