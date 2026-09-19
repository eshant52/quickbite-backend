package com.quickbite.quickbite.auth.service;

import com.quickbite.quickbite.auth.dto.DeviceInfo;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SessionPersistenceServiceImpl implements SessionPersistenceService {

    private final SessionRepository sessionRepository;
    private final RefreshTokenFamilyRepository refreshTokenFamilyRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ChallengeTokenService challengeTokenService;
    private final AuthProperties authProperties;

    public SessionPersistenceServiceImpl(
            SessionRepository sessionRepository,
            RefreshTokenFamilyRepository refreshTokenFamilyRepository,
            RefreshTokenRepository refreshTokenRepository,
            ChallengeTokenService challengeTokenService,
            AuthProperties authProperties) {
        this.sessionRepository = sessionRepository;
        this.refreshTokenFamilyRepository = refreshTokenFamilyRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.challengeTokenService = challengeTokenService;
        this.authProperties = authProperties;
    }

    @Override
    public Session persistNewSession(User user, DeviceInfo deviceInfo, Instant expiresAt, String rawToken) {
        UUID userId = user.getId();
        long activeCount = sessionRepository.countActiveByUserId(userId);

        if (activeCount >= authProperties.maxConcurrentSessions()) {
            String challengeToken = challengeTokenService.generateSessionLimitChallenge(userId);
            throw new MaxSessionException(challengeToken, authProperties.maxConcurrentSessions());
        }

        Instant now = Instant.now();

        // 1. Create Session
        Session session = new Session();
        session.setUser(user);
        session.setDeviceName(deviceInfo.deviceName());
        session.setDeviceOS(deviceInfo.deviceOs());
        session.setClientType(deviceInfo.clientType());
        session.setIp(deviceInfo.ip());
        session.setUserAgent(deviceInfo.userAgent());
        session.setLoginAt(now);
        session.setLastUsedAt(now);
        session.setExpiresAt(expiresAt);
        session = sessionRepository.save(session);

        // 2. Create RefreshTokenFamily
        RefreshTokenFamily family = new RefreshTokenFamily();
        family.setSession(session);
        family = refreshTokenFamilyRepository.save(family);

        // 3. Create initial RefreshToken (generation = 1)
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setFamily(family);
        refreshToken.setTokenHash(TokenUtils.sha256(rawToken));
        refreshToken.setGeneration(1);
        refreshToken.setExpiresAt(expiresAt);
        refreshTokenRepository.save(refreshToken);

        return session;
    }

    @Override
    public boolean markTokenUsed(UUID tokenId, Instant now) {
        return refreshTokenRepository.markTokenUsed(tokenId, now) > 0;
    }

    @Override
    public void saveRotatedToken(RefreshTokenFamily family, Session session, String newRawToken, int nextGeneration, Instant expiresAt) {
        RefreshToken nextToken = new RefreshToken();
        nextToken.setFamily(family);
        nextToken.setTokenHash(TokenUtils.sha256(newRawToken));
        nextToken.setGeneration(nextGeneration);
        nextToken.setExpiresAt(expiresAt);
        refreshTokenRepository.save(nextToken);

        sessionRepository.updateLastUsed(session.getId(), Instant.now());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeBreachedFamily(UUID familyId, UUID sessionId, UUID userId) {
        refreshTokenFamilyRepository.revokeFamilyOnBreach(familyId);
        sessionRepository.revokeSessionByIdAndUserId(sessionId, userId);
    }

    @Override
    public int revokeSession(UUID userId, UUID sessionId) {
        refreshTokenFamilyRepository.revokeFamiliesBySessionIdAndUserId(sessionId, userId);
        return sessionRepository.revokeSessionByIdAndUserId(sessionId, userId);
    }

    @Override
    public int revokeAllSessions(UUID userId) {
        refreshTokenFamilyRepository.revokeFamiliesByUserId(userId);
        return sessionRepository.revokeAllByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Session> findActiveSessionsByUserId(UUID userId) {
        return sessionRepository.findActiveByUserId(userId);
    }
}
