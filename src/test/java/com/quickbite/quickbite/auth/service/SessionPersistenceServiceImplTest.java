package com.quickbite.quickbite.auth.service;

import com.quickbite.quickbite.auth.dto.DeviceInfo;
import com.quickbite.quickbite.auth.exception.MaxSessionException;
import com.quickbite.quickbite.auth.model.ClientType;
import com.quickbite.quickbite.auth.model.RefreshToken;
import com.quickbite.quickbite.auth.model.RefreshTokenFamily;
import com.quickbite.quickbite.auth.model.Session;
import com.quickbite.quickbite.auth.repository.RefreshTokenFamilyRepository;
import com.quickbite.quickbite.auth.repository.RefreshTokenRepository;
import com.quickbite.quickbite.auth.repository.SessionRepository;
import com.quickbite.quickbite.auth.service.token.ChallengeTokenService;
import com.quickbite.quickbite.common.config.property.AuthProperties;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionPersistenceServiceImplTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private RefreshTokenFamilyRepository refreshTokenFamilyRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private ChallengeTokenService challengeTokenService;

    private AuthProperties authProperties;
    private SessionPersistenceServiceImpl persistenceService;

    private User user;
    private DeviceInfo deviceInfo;
    private UUID userId;

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

        persistenceService = new SessionPersistenceServiceImpl(
                sessionRepository,
                refreshTokenFamilyRepository,
                refreshTokenRepository,
                challengeTokenService,
                authProperties
        );

        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);

        deviceInfo = new DeviceInfo("MacBook Pro", "macOS", "15.0", ClientType.WEB_BROWSER, "127.0.0.1", "Mozilla/5.0");
    }

    @Test
    @DisplayName("persistNewSession - persists session, family, and token when under limit")
    void persistNewSession_success() {
        when(sessionRepository.countActiveByUserId(userId)).thenReturn(1L);

        Session savedSession = new Session();
        savedSession.setId(UUID.randomUUID());
        savedSession.setUser(user);
        when(sessionRepository.save(any(Session.class))).thenReturn(savedSession);

        RefreshTokenFamily savedFamily = new RefreshTokenFamily();
        savedFamily.setId(UUID.randomUUID());
        savedFamily.setSession(savedSession);
        when(refreshTokenFamilyRepository.save(any(RefreshTokenFamily.class))).thenReturn(savedFamily);

        Instant expiresAt = Instant.now().plus(Duration.ofDays(7));
        String rawToken = "raw-opaque-token-value";

        Session result = persistenceService.persistNewSession(user, deviceInfo, expiresAt, rawToken);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(savedSession.getId());

        verify(sessionRepository).save(any(Session.class));
        verify(refreshTokenFamilyRepository).save(any(RefreshTokenFamily.class));

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken capturedToken = tokenCaptor.getValue();
        assertThat(capturedToken.getGeneration()).isEqualTo(1);
        assertThat(capturedToken.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(capturedToken.getTokenHash()).isNotBlank();
    }

    @Test
    @DisplayName("persistNewSession - throws MaxSessionException when active sessions reach limit")
    void persistNewSession_limitExceeded() {
        when(sessionRepository.countActiveByUserId(userId)).thenReturn(3L);
        when(challengeTokenService.generateSessionLimitChallenge(userId)).thenReturn("mock.challenge.jwt");

        Instant expiresAt = Instant.now().plus(Duration.ofDays(7));
        String rawToken = "raw-opaque-token-value";

        assertThatThrownBy(() -> persistenceService.persistNewSession(user, deviceInfo, expiresAt, rawToken))
                .isInstanceOf(MaxSessionException.class)
                .matches(ex -> "mock.challenge.jwt".equals(((MaxSessionException) ex).getSessionManagementToken()));

        verify(sessionRepository, never()).save(any());
        verify(refreshTokenFamilyRepository, never()).save(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("revokeBreachedFamily - revokes family and session by user ID")
    void revokeBreachedFamily_success() {
        UUID familyId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        persistenceService.revokeBreachedFamily(familyId, sessionId, userId);

        verify(refreshTokenFamilyRepository).revokeFamilyOnBreach(familyId);
        verify(sessionRepository).revokeSessionByIdAndUserId(sessionId, userId);
    }

    @Test
    @DisplayName("revokeSession - revokes family and session scoped by user ID")
    void revokeSession_success() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.revokeSessionByIdAndUserId(sessionId, userId)).thenReturn(1);

        int result = persistenceService.revokeSession(userId, sessionId);

        assertThat(result).isEqualTo(1);
        verify(refreshTokenFamilyRepository).revokeFamiliesBySessionIdAndUserId(sessionId, userId);
        verify(sessionRepository).revokeSessionByIdAndUserId(sessionId, userId);
    }

    @Test
    @DisplayName("revokeAllSessions - revokes all families and sessions for user")
    void revokeAllSessions_success() {
        when(sessionRepository.revokeAllByUserId(userId)).thenReturn(2);

        int result = persistenceService.revokeAllSessions(userId);

        assertThat(result).isEqualTo(2);
        verify(sessionRepository).revokeAllByUserId(userId);
        verify(refreshTokenFamilyRepository).revokeFamiliesByUserId(userId);
    }

    @Test
    @DisplayName("markTokenUsed - delegates to repository")
    void markTokenUsed_success() {
        UUID tokenId = UUID.randomUUID();
        Instant now = Instant.now();
        when(refreshTokenRepository.markTokenUsed(tokenId, now)).thenReturn(1);

        boolean marked = persistenceService.markTokenUsed(tokenId, now);

        assertThat(marked).isTrue();
    }

    @Test
    @DisplayName("saveRotatedToken - saves next token and updates session lastUsedAt")
    void saveRotatedToken_success() {
        RefreshTokenFamily family = new RefreshTokenFamily();
        family.setId(UUID.randomUUID());

        Session session = new Session();
        session.setId(UUID.randomUUID());

        Instant expiresAt = Instant.now().plus(Duration.ofDays(7));
        String newRawToken = "new-raw-token";

        persistenceService.saveRotatedToken(family, session, newRawToken, 2, expiresAt);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getGeneration()).isEqualTo(2);

        verify(sessionRepository).updateLastUsed(eq(session.getId()), any(Instant.class));
    }
}
