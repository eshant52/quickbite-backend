package com.quickbite.quickbite.auth.controller;

import com.quickbite.quickbite.auth.dto.AuthenticatedSession;
import com.quickbite.quickbite.auth.service.AuthCookieService;
import com.quickbite.quickbite.auth.service.LogoutService;
import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutControllerTest {

    @Mock
    private LogoutService logoutService;

    @Mock
    private AuthCookieService authCookieService;

    @Mock
    private AuthenticatedSessionResolver authenticatedSessionResolver;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private LogoutController controller;

    private UUID userId;
    private UUID sessionId;
    private AuthenticatedSession authenticatedSession;
    private ResponseCookie expiredCookie;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        authenticatedSession = new AuthenticatedSession(userId, sessionId);
        expiredCookie = ResponseCookie.from("refreshToken", "").maxAge(0).build();
        when(authCookieService.expiredRefreshCookie()).thenReturn(expiredCookie);
    }

    @Test
    @DisplayName("logout - terminates current session and expires cookie")
    void logout_success() {
        when(authenticatedSessionResolver.currentSession(jwt)).thenReturn(authenticatedSession);

        ResponseEntity<Void> response = controller.logout(jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getHeaders().getFirst("Set-Cookie")).isNotNull();
        verify(logoutService).logoutCurrentSession(userId, sessionId);
    }

    @Test
    @DisplayName("logoutAll - terminates all sessions and expires cookie")
    void logoutAll_success() {
        when(authenticatedSessionResolver.currentSession(jwt)).thenReturn(authenticatedSession);

        ResponseEntity<Void> response = controller.logoutAll(jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getHeaders().getFirst("Set-Cookie")).isNotNull();
        verify(logoutService).logoutAllSessions(userId);
    }
}
