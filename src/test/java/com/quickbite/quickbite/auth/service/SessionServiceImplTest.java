package com.quickbite.quickbite.auth.service;

import com.quickbite.quickbite.auth.dto.DeviceInfo;
import com.quickbite.quickbite.auth.dto.IssuedToken;
import com.quickbite.quickbite.auth.dto.SessionResponse;
import com.quickbite.quickbite.auth.exception.AuthenticationException;
import com.quickbite.quickbite.auth.model.ClientType;
import com.quickbite.quickbite.auth.model.RefreshToken;
import com.quickbite.quickbite.auth.model.RefreshTokenFamily;
import com.quickbite.quickbite.auth.model.Session;
import com.quickbite.quickbite.auth.repository.RefreshTokenRepository;
import com.quickbite.quickbite.auth.util.TokenUtils;
import com.quickbite.quickbite.common.config.property.AuthProperties;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private SessionPersistenceService sessionPersistenceService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private SessionStoreService sessionStoreService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    private AuthProperties authProperties;
    private SessionServiceImpl sessionService;

    private User user;
    private DeviceInfo deviceInfo;
    private UUID userId;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        AuthProperties.JwtProperties jwtProperties = new AuthProperties.JwtProperties(
                "classpath:certs/private.pem",
                "classpath:certs/public.pem",
                Duration.ofMinutes(15),
                "quickbite-api",
                Duration.ofDays(7),
                Duration.ofMinutes(5),
                "quickbite-auth"
        );
        authProperties = new AuthProperties(3, null, jwtProperties, null);

        sessionService = new SessionServiceImpl(
                sessionPersistenceService,
                refreshTokenRepository,
                sessionStoreService,
                authProperties,
                redissonClient
        );

        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);

        deviceInfo = new DeviceInfo("MacBook Pro", "macOS", "15.0", ClientType.WEB_BROWSER, "127.0.0.1", "Mozilla/5.0");
    }

    @Test
    @DisplayName("createNewSession - successfully locks, persists session, updates Redis, and releases lock")
    void createNewSession_success() throws InterruptedException {
        when(redissonClient.getLock("quickbite:session-lock:" + userId)).thenReturn(rLock);
        when(rLock.tryLock(eq(0L), eq(10L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        Session session = new Session();
        session.setId(sessionId);
        session.setUser(user);

        when(sessionPersistenceService.persistNewSession(eq(user), eq(deviceInfo), any(Instant.class), anyString()))
                .thenReturn(session);

        IssuedToken issuedToken = sessionService.createNewSession(user, deviceInfo);

        assertThat(issuedToken).isNotNull();
        assertThat(issuedToken.sessionId()).isEqualTo(sessionId);
        assertThat(issuedToken.userId()).isEqualTo(userId);
        assertThat(issuedToken.rawToken()).isNotBlank();

        verify(rLock).unlock();
    }

    @Test
    @DisplayName("createNewSession - throws AuthenticationException when lock cannot be acquired")
    void createNewSession_lockContention() throws InterruptedException {
        when(redissonClient.getLock("quickbite:session-lock:" + userId)).thenReturn(rLock);
        when(rLock.tryLock(eq(0L), eq(10L), eq(TimeUnit.SECONDS))).thenReturn(false);

        assertThatThrownBy(() -> sessionService.createNewSession(user, deviceInfo))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Session creation is already in progress");

        verifyNoInteractions(sessionPersistenceService);
        verify(rLock, never()).unlock();
    }

    @Test
    @DisplayName("createNewSession - unlocks in finally even if persistence throws an exception")
    void createNewSession_unlocksInFinallyOnException() throws InterruptedException {
        when(redissonClient.getLock("quickbite:session-lock:" + userId)).thenReturn(rLock);
        when(rLock.tryLock(eq(0L), eq(10L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        when(sessionPersistenceService.persistNewSession(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> sessionService.createNewSession(user, deviceInfo))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB error");

        verify(rLock).unlock();
        verifyNoInteractions(sessionStoreService);
    }

    @Test
    @DisplayName("validateAndRotate - rotates token successfully and caches in grace cache")
    void validateAndRotate_success() throws InterruptedException {
        String rawToken = "raw-refresh-token-123";
        String hash = TokenUtils.sha256(rawToken);

        Session session = new Session();
        session.setId(sessionId);
        session.setUser(user);
        session.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));

        RefreshTokenFamily family = new RefreshTokenFamily();
        family.setId(UUID.randomUUID());
        family.setSession(session);

        RefreshToken existing = new RefreshToken();
        existing.setId(UUID.randomUUID());
        existing.setFamily(family);
        existing.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
        existing.setGeneration(1);

        when(sessionStoreService.getRotatedTokenGrace(hash)).thenReturn(Optional.empty());
        when(redissonClient.getLock("quickbite:refresh-lock:" + hash)).thenReturn(rLock);
        when(rLock.tryLock(eq(3L), eq(5L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        when(refreshTokenRepository.findRefreshTokenWithFamilyAndSessionByTokenHash(hash))
                .thenReturn(Optional.of(existing));
        when(sessionPersistenceService.markTokenUsed(eq(existing.getId()), any(Instant.class)))
                .thenReturn(true);

        IssuedToken issuedToken = sessionService.validateAndRotate(rawToken);

        assertThat(issuedToken).isNotNull();
        assertThat(issuedToken.sessionId()).isEqualTo(sessionId);
        assertThat(issuedToken.userId()).isEqualTo(userId);
        assertThat(issuedToken.rawToken()).isNotBlank();

        verify(sessionPersistenceService).saveRotatedToken(eq(family), eq(session), anyString(), eq(2), eq(existing.getExpiresAt()));
        verify(sessionStoreService).cacheRotatedTokenGrace(eq(hash), anyString(), eq(Duration.ofSeconds(2)));
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("validateAndRotate - throws AuthenticationException if user account is deactivated")
    void validateAndRotate_deactivatedUser() throws InterruptedException {
        String rawToken = "raw-refresh-token-123";
        String hash = TokenUtils.sha256(rawToken);

        user.setActive(false);

        Session session = new Session();
        session.setId(sessionId);
        session.setUser(user);
        session.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));

        RefreshTokenFamily family = new RefreshTokenFamily();
        family.setId(UUID.randomUUID());
        family.setSession(session);

        RefreshToken existing = new RefreshToken();
        existing.setId(UUID.randomUUID());
        existing.setFamily(family);
        existing.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));

        when(sessionStoreService.getRotatedTokenGrace(hash)).thenReturn(Optional.empty());
        when(redissonClient.getLock("quickbite:refresh-lock:" + hash)).thenReturn(rLock);
        when(rLock.tryLock(eq(3L), eq(5L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        when(refreshTokenRepository.findRefreshTokenWithFamilyAndSessionByTokenHash(hash))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> sessionService.validateAndRotate(rawToken))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Account is deactivated");

        verifyNoInteractions(sessionPersistenceService);
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("validateAndRotate - fast path returns cached token before locking when already in grace cache")
    void validateAndRotate_fastPathGraceReplay() {
        String rawToken = "raw-refresh-token-123";
        String hash = TokenUtils.sha256(rawToken);
        String cachedNewRawToken = "cached-new-raw-token-456";

        Session session = new Session();
        session.setId(sessionId);
        session.setUser(user);

        RefreshTokenFamily family = new RefreshTokenFamily();
        family.setSession(session);

        RefreshToken token = new RefreshToken();
        token.setFamily(family);

        when(sessionStoreService.getRotatedTokenGrace(hash)).thenReturn(Optional.of(cachedNewRawToken));
        when(refreshTokenRepository.findRefreshTokenWithFamilyAndSessionByTokenHash(hash)).thenReturn(Optional.of(token));

        IssuedToken issuedToken = sessionService.validateAndRotate(rawToken);

        assertThat(issuedToken).isNotNull();
        assertThat(issuedToken.rawToken()).isEqualTo(cachedNewRawToken);
        assertThat(issuedToken.sessionId()).isEqualTo(sessionId);
        assertThat(issuedToken.userId()).isEqualTo(userId);

        verifyNoInteractions(redissonClient);
        verifyNoInteractions(sessionPersistenceService);
    }

    @Test
    @DisplayName("validateAndRotate - 5ms race resolved: double-check finds cached token after acquiring lock")
    void validateAndRotate_concurrent5msDoubleCheckGraceHit() throws InterruptedException {
        String rawToken = "raw-refresh-token-123";
        String hash = TokenUtils.sha256(rawToken);
        String cachedNewRawToken = "cached-new-raw-token-456";

        Session session = new Session();
        session.setId(sessionId);
        session.setUser(user);

        RefreshTokenFamily family = new RefreshTokenFamily();
        family.setSession(session);

        RefreshToken token = new RefreshToken();
        token.setFamily(family);

        // 1. Fast path: cache was empty when T2 arrived (T1 was still writing)
        // 2. Double-check inside lock: cache is populated because T1 just finished!
        when(sessionStoreService.getRotatedTokenGrace(hash))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(cachedNewRawToken));

        when(redissonClient.getLock("quickbite:refresh-lock:" + hash)).thenReturn(rLock);
        when(rLock.tryLock(eq(3L), eq(5L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        when(refreshTokenRepository.findRefreshTokenWithFamilyAndSessionByTokenHash(hash)).thenReturn(Optional.of(token));

        IssuedToken issuedToken = sessionService.validateAndRotate(rawToken);

        assertThat(issuedToken).isNotNull();
        assertThat(issuedToken.rawToken()).isEqualTo(cachedNewRawToken);
        assertThat(issuedToken.sessionId()).isEqualTo(sessionId);
        assertThat(issuedToken.userId()).isEqualTo(userId);

        verify(rLock).unlock();
        // Breach must NOT be triggered
        verify(sessionPersistenceService, never()).revokeBreachedFamily(any(), any(), any());
        verify(sessionPersistenceService, never()).markTokenUsed(any(), any());
    }

    @Test
    @DisplayName("validateAndRotate - triggers breach and commits revocation via REQUIRES_NEW when reuse is outside grace window")
    void validateAndRotate_breachDetection() throws InterruptedException {
        String rawToken = "raw-refresh-token-123";
        String hash = TokenUtils.sha256(rawToken);
        UUID familyId = UUID.randomUUID();

        Session session = new Session();
        session.setId(sessionId);
        session.setUser(user);
        session.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));

        RefreshTokenFamily family = new RefreshTokenFamily();
        family.setId(familyId);
        family.setSession(session);

        RefreshToken existing = new RefreshToken();
        existing.setId(UUID.randomUUID());
        existing.setFamily(family);
        existing.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));

        when(sessionStoreService.getRotatedTokenGrace(hash)).thenReturn(Optional.empty());
        when(redissonClient.getLock("quickbite:refresh-lock:" + hash)).thenReturn(rLock);
        when(rLock.tryLock(eq(3L), eq(5L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        when(refreshTokenRepository.findRefreshTokenWithFamilyAndSessionByTokenHash(hash))
                .thenReturn(Optional.of(existing));
        when(sessionPersistenceService.markTokenUsed(eq(existing.getId()), any(Instant.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> sessionService.validateAndRotate(rawToken))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Refresh token reuse detected");

        // Independent transaction revocation must be called
        verify(sessionPersistenceService).revokeBreachedFamily(familyId, sessionId, userId);
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("validateAndRotate - throws AuthenticationException when refresh lock cannot be acquired")
    void validateAndRotate_lockContention() throws InterruptedException {
        String rawToken = "raw-refresh-token-123";
        String hash = TokenUtils.sha256(rawToken);

        when(sessionStoreService.getRotatedTokenGrace(hash)).thenReturn(Optional.empty());
        when(redissonClient.getLock("quickbite:refresh-lock:" + hash)).thenReturn(rLock);
        when(rLock.tryLock(eq(3L), eq(5L), eq(TimeUnit.SECONDS))).thenReturn(false);

        assertThatThrownBy(() -> sessionService.validateAndRotate(rawToken))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Token rotation already in progress");

        verify(rLock, never()).unlock();
    }

    @Test
    @DisplayName("revokeSession - delegates to persistence service")
    void revokeSession_delegatesToPersistence() {
        sessionService.revokeSession(userId, sessionId);

        verify(sessionPersistenceService).revokeSession(userId, sessionId);
    }

    @Test
    @DisplayName("revokeAllSessions - delegates to persistence service")
    void revokeAllSessions_delegatesToPersistence() {
        sessionService.revokeAllSessions(userId);

        verify(sessionPersistenceService).revokeAllSessions(userId);
    }

    @Test
    @DisplayName("listActiveSessionsForUser - returns mapped sessions")
    void listActiveSessionsForUser_success() {
        Session session = new Session();
        session.setId(sessionId);
        session.setDeviceName("iPhone");
        session.setDeviceOS("iOS");
        session.setClientType(ClientType.MOBILE_APP);
        session.setIp("192.168.1.1");
        session.setLastUsedAt(Instant.now());
        session.setLoginAt(Instant.now());
        session.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));

        when(sessionPersistenceService.findActiveSessionsByUserId(userId)).thenReturn(List.of(session));

        List<SessionResponse> responses = sessionService.listActiveSessionsForUser(userId);

        assertThat(responses).hasSize(1);
        SessionResponse response = responses.getFirst();
        assertThat(response.sessionId()).isEqualTo(sessionId);
        assertThat(response.deviceName()).isEqualTo("iPhone");
        assertThat(response.clientType()).isEqualTo("MOBILE_APP");
        assertThat(response.daysLeft()).isGreaterThanOrEqualTo(6);
    }
}
