package com.quickbite.quickbite.auth.controller;

import com.quickbite.quickbite.auth.dto.AuthResponse;
import com.quickbite.quickbite.auth.dto.DeviceInfo;
import com.quickbite.quickbite.auth.dto.SessionResponse;
import com.quickbite.quickbite.auth.model.ClientType;
import com.quickbite.quickbite.auth.service.AuthCookieService;
import com.quickbite.quickbite.auth.service.SessionChallengeService;
import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.auth.util.DeviceInfoResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionChallengeControllerTest {

    @Mock
    private SessionChallengeService sessionChallengeService;

    @Mock
    private AuthCookieService authCookieService;

    @Mock
    private AuthenticatedSessionResolver authenticatedSessionResolver;

    @Mock
    private DeviceInfoResolver deviceInfoResolver;

    @Mock
    private Jwt jwt;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private SessionChallengeController controller;

    private UUID userId;
    private UUID sessionId;
    private DeviceInfo deviceInfo;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        deviceInfo = new DeviceInfo("QuickBiteApp", "iOS", "18.0", ClientType.MOBILE_APP, "127.0.0.1", "QuickBite/1.0");
    }

    @Test
    @DisplayName("listSessions - returns active sessions for challenge token")
    void listSessions_success() {
        SessionResponse session = new SessionResponse(sessionId, "QuickBiteApp", "iOS 18.0", "MOBILE_APP", "127.0.0.1", Instant.now(), Instant.now(), 30);
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);
        when(sessionChallengeService.getActiveSessionsForUser(userId)).thenReturn(List.of(session));

        ResponseEntity<List<SessionResponse>> response = controller.listSessions(jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().sessionId()).isEqualTo(sessionId);
    }

    @Test
    @DisplayName("claimSession - successfully claims a session slot")
    void claimSession_success() {
        AuthResponse authResponse = new AuthResponse("access-token-123", "refresh-token-456", 900L, sessionId);
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);
        when(deviceInfoResolver.resolve(any())).thenReturn(deviceInfo);
        when(sessionChallengeService.claimSession(userId, deviceInfo)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = controller.claimSession(jwt, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(authResponse);
    }

    @Test
    @DisplayName("revokeSession - terminates selected session")
    void revokeSession_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);

        ResponseEntity<Void> response = controller.revokeSession(jwt, sessionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(sessionChallengeService).revokeSession(userId, sessionId);
    }
}
