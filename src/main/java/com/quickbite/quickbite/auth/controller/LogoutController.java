package com.quickbite.quickbite.auth.controller;

import com.quickbite.quickbite.auth.dto.AuthenticatedSession;
import com.quickbite.quickbite.auth.service.AuthCookieService;
import com.quickbite.quickbite.auth.service.LogoutService;
import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@PreAuthorize("hasAuthority('SCOPE_API')")
public class LogoutController {

    private final LogoutService logoutService;
    private final AuthCookieService authCookieService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;

    public LogoutController(
            LogoutService logoutService,
            AuthCookieService authCookieService,
            AuthenticatedSessionResolver authenticatedSessionResolver) {
        this.logoutService = logoutService;
        this.authCookieService = authCookieService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        AuthenticatedSession session = authenticatedSessionResolver.currentSession(jwt);
        logoutService.logoutCurrentSession(session.userId(), session.sessionId());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.expiredRefreshCookie().toString())
                .build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal Jwt jwt) {
        logoutService.logoutAllSessions(authenticatedSessionResolver.currentSession(jwt).userId());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.expiredRefreshCookie().toString())
                .build();
    }
}
