package com.quickbite.quickbite.services.auth;

import com.quickbite.quickbite.dtos.auth.DeviceInfo;
import com.quickbite.quickbite.dtos.auth.IssuedToken;
import com.quickbite.quickbite.exceptions.AuthenticationException;
import com.quickbite.quickbite.exceptions.MaxSessionException;
import com.quickbite.quickbite.models.RefreshToken;
import com.quickbite.quickbite.models.User;
import com.quickbite.quickbite.repositories.RefreshTokenRepository;
import com.quickbite.quickbite.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SessionServiceImpl implements SessionService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionStoreService sessionStoreService;
    private final SessionManagementTokenStoreService sessionManagementTokenStoreService;

    @Value("${quickbite.jwt.refresh-token-expiry}")
    private String refreshTokenExpiry;

    @Value("${quickbite.auth.max-concurrent-sessions}")
    private int maxConcurrentSessions;

    public SessionServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            SessionStoreService sessionStoreService,
            SessionManagementTokenStoreService sessionManagementTokenStoreService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.sessionStoreService = sessionStoreService;
        this.sessionManagementTokenStoreService = sessionManagementTokenStoreService;
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

            UUID familyId = UUID.randomUUID();
            String rawToken = TokenUtils.generateOpaqueToken();

            persistToken(user, familyId, rawToken, deviceInfo);
            sessionStoreService.addSession(userId, familyId, Duration.parse(refreshTokenExpiry));

            return new IssuedToken(rawToken, familyId, user.getId());
        } finally {
            sessionStoreService.releaseSessionCreationLock(userId);
        }
    }

    @Override
    @Transactional
    public IssuedToken validateAndRotate(String rawToken) {
        String hash = TokenUtils.sha256(rawToken);

        RefreshToken existing = refreshTokenRepository.findRefreshTokenByTokenHash(hash)
                .orElseThrow(() -> new AuthenticationException("Invalid or expired refresh token"));

        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthenticationException("Refresh token has expired");
        }

        int rowsAffected = refreshTokenRepository.atomicRevokeIfActive(hash, Instant.now());

        if (rowsAffected == 0) {
            // Someone else already rotated this exact token before us.
            // Either a legitimate concurrent retry lost the race (acceptable,
            // client should retry with the newer token it then receives from
            // the winning request) or this token was replayed after rotation
            // (theft). We cannot distinguish these here with certainty, so
            // we treat it conservatively: revoke the family.

            refreshTokenRepository.revokeFamily(existing.getFamilyId());
            sessionStoreService.removeSession(existing.getUser().getId(), existing.getFamilyId());
            throw new AuthenticationException(
                    "Refresh token reuse detected, session have been revoked. Please login again."
            );
        }

        String newRawToken = TokenUtils.generateOpaqueToken();
        existing.setLastUsedAt(Instant.now());
        DeviceInfo deviceInfo = new DeviceInfo(existing.getDeviceName(), existing.getOs(), existing.getClientType());
        persistToken(existing.getUser(), existing.getFamilyId(), newRawToken, deviceInfo);
        return new IssuedToken(newRawToken, existing.getFamilyId(), existing.getUser().getId());
    }

    @Override
    @Transactional
    public void revokeSession(UUID userId, UUID familyId) {
        refreshTokenRepository.revokeFamily(familyId);
        sessionStoreService.removeSession(userId, familyId);
    }

    @Override
    @Transactional
    public void revokeAllSessions(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId);
        sessionStoreService.removeAllSessions(userId);
    }

    @Override
    public List<RefreshToken> listActiveSessionsForUser(UUID userId) {
        return refreshTokenRepository.findRefreshTokensByUserIdAndRevokedIsFalseOrderByCreatedAtAsc(userId);
    }

    /**
     *  Helper function
     */

    private void enforceSessionLimit(UUID userId) {
        long activeCount = sessionStoreService.getActiveSessionsCount(userId);

        if (activeCount >= maxConcurrentSessions) {
            String sessionManagementToken = sessionManagementTokenStoreService.createToken(userId);
            throw new MaxSessionException(sessionManagementToken, maxConcurrentSessions);
        }
    }

    private void persistToken(User user, UUID familyId, String rawToken, DeviceInfo deviceInfo) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setFamilyId(familyId);
        refreshToken.setDeviceName(deviceInfo.deviceName());
        refreshToken.setOs(deviceInfo.os());
        refreshToken.setClientType(deviceInfo.clientType());
        refreshToken.setTokenHash(TokenUtils.sha256(rawToken));
        refreshToken.setExpiresAt(Instant.now().plusSeconds(Duration.parse(refreshTokenExpiry).toSeconds()));
        refreshToken.setLastUsedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);
    }
}
