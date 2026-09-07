package com.quickbite.quickbite.auth.service;

import com.quickbite.quickbite.auth.dto.DeviceInfo;
import com.quickbite.quickbite.auth.dto.IssuedToken;
import com.quickbite.quickbite.auth.dto.SessionResponse;
import com.quickbite.quickbite.auth.exception.AuthenticationException;
import com.quickbite.quickbite.auth.exception.MaxSessionException;
import com.quickbite.quickbite.auth.model.RefreshToken;
import com.quickbite.quickbite.auth.model.RefreshTokenFamily;
import com.quickbite.quickbite.auth.model.Session;
import com.quickbite.quickbite.auth.repository.RefreshTokenFamilyRepository;
import com.quickbite.quickbite.auth.repository.RefreshTokenRepository;
import com.quickbite.quickbite.auth.repository.SessionRepository;
import com.quickbite.quickbite.auth.service.token.ChallengeTokenService;
import com.quickbite.quickbite.auth.util.TokenUtils;
import com.quickbite.quickbite.common.config.property.AuthProperties;
import com.quickbite.quickbite.user.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final RefreshTokenFamilyRepository refreshTokenFamilyRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionStoreService sessionStoreService;
    private final ChallengeTokenService challengeTokenService;
    private final AuthProperties authProperties;


    public SessionServiceImpl(
            SessionRepository sessionRepository,
            RefreshTokenFamilyRepository refreshTokenFamilyRepository,
            RefreshTokenRepository refreshTokenRepository,
            SessionStoreService sessionStoreService,
            ChallengeTokenService challengeTokenService,
            AuthProperties authProperties) {
        this.sessionRepository = sessionRepository;
        this.refreshTokenFamilyRepository = refreshTokenFamilyRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.sessionStoreService = sessionStoreService;
        this.challengeTokenService = challengeTokenService;
        this.authProperties = authProperties;
    }

    @Override
    @Transactional
    public IssuedToken createNewSession(User user, DeviceInfo deviceInfo) {
        UUID userId = user.getId();
        if (!sessionStoreService.acquireSessionCreationLock(userId, Duration.ofSeconds(10))) {
            throw new AuthenticationException("Session creation is already in progress. Please try again.");
        }

        try {
            enforceSessionLimit(userId);

            Instant now = Instant.now();
            Instant expiresAt = now.plus(authProperties.jwt().refreshTokenExpiry());

            // 1. Create Session
            Session session = new Session();
            session.setUser(user);
            session.setDeviceName(deviceInfo.deviceName());
            session.setDeviceOS(deviceInfo.deviceOs());
            session.setClientType(deviceInfo.clientType());
            session.setIp(deviceInfo.ip());
            session.setLoginAt(now);
            session.setLastUsedAt(now);
            session.setExpiresAt(expiresAt);
            session = sessionRepository.save(session);

            // 2. Create RefreshTokenFamily
            RefreshTokenFamily family = new RefreshTokenFamily();
            family.setSession(session);
            family = refreshTokenFamilyRepository.save(family);

            // 3. Create initial RefreshToken (generation = 1)
            String rawToken = TokenUtils.generateOpaqueToken();
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setFamily(family);
            refreshToken.setTokenHash(TokenUtils.sha256(rawToken));
            refreshToken.setGeneration(1);
            refreshToken.setExpiresAt(expiresAt);
            refreshTokenRepository.save(refreshToken);

            // 4. Track in Redis
            sessionStoreService.addSession(userId, session.getId(), authProperties.jwt().refreshTokenExpiry());

            return new IssuedToken(rawToken, session.getId(), user.getId());
        } finally {
            sessionStoreService.releaseSessionCreationLock(userId);
        }
    }

    @Override
    @Transactional
    public IssuedToken validateAndRotate(String rawToken) {
        String hash = TokenUtils.sha256(rawToken);

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

        // Atomic update: try marking the token used
        int marked = refreshTokenRepository.markTokenUsed(existing.getId(), Instant.now());

        if (marked == 0) {
            // BREACH DETECTED! Old token presented after rotation
            refreshTokenFamilyRepository.revokeFamilyOnBreach(family.getId());
            sessionRepository.revokeSessionById(session.getId());
            sessionStoreService.removeSession(session.getUser().getId(), session.getId());

            throw new AuthenticationException("Refresh token reuse detected. Session has been revoked. Please login again.");
        }

        // SAFE ROTATION PATH
        String newRawToken = TokenUtils.generateOpaqueToken();
        RefreshToken nextToken = new RefreshToken();
        nextToken.setFamily(family);
        nextToken.setTokenHash(TokenUtils.sha256(newRawToken));
        nextToken.setGeneration(existing.getGeneration() + 1);
        nextToken.setExpiresAt(existing.getExpiresAt());
        refreshTokenRepository.save(nextToken);

        sessionRepository.updateLastUsed(session.getId(), Instant.now());

        return new IssuedToken(newRawToken, session.getId(), session.getUser().getId());
    }

    @Override
    @Transactional
    public void revokeSession(UUID userId, UUID sessionId) {
        refreshTokenFamilyRepository.revokeFamiliesBySessionId(sessionId);
        sessionRepository.revokeSessionById(sessionId);
        sessionStoreService.removeSession(userId, sessionId);
    }

    @Override
    @Transactional
    public void revokeAllSessions(UUID userId) {
        sessionRepository.revokeAllByUserId(userId);
        refreshTokenFamilyRepository.revokeFamiliesByUserId(userId);
        sessionStoreService.removeAllSessions(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> listActiveSessionsForUser(UUID userId) {
        List<Session> sessions = sessionRepository.findActiveByUserId(userId);
        return sessions.stream()
                .map(s -> new SessionResponse(
                        s.getId(),
                        s.getDeviceName(),
                        s.getDeviceOS(),
                        s.getClientType() != null ? s.getClientType().name() : null,
                        s.getIp(),
                        s.getLastUsedAt(),
                        s.getLoginAt(),
                        Math.max(0, (int) Duration.between(Instant.now(), s.getExpiresAt()).toDays())
                ))
                .toList();
    }

    private void enforceSessionLimit(UUID userId) {
        long activeCount = sessionStoreService.getActiveSessionsCount(userId);

        if (activeCount >= authProperties.maxConcurrentSessions()) {
            String challengeToken = challengeTokenService.generateSessionLimitChallenge(userId);
            throw new MaxSessionException(challengeToken, authProperties.maxConcurrentSessions());
        }
    }
}
