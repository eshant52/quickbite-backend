package com.quickbite.quickbite.auth.controller;

import com.quickbite.quickbite.auth.dto.AuthResponse;
import com.quickbite.quickbite.auth.dto.ClientRequestMetadata;
import com.quickbite.quickbite.auth.dto.DeviceInfo;
import com.quickbite.quickbite.auth.dto.SessionResponse;
import com.quickbite.quickbite.auth.model.ClientType;
import com.quickbite.quickbite.auth.service.AuthCookieService;
import com.quickbite.quickbite.auth.service.SessionChallengeService;
import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.auth.util.DeviceInfoResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@PreAuthorize("hasAuthority('SCOPE_AUTH')")
public class SessionChallengeController {

    private final SessionChallengeService sessionChallengeService;
    private final AuthCookieService authCookieService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;
    private final DeviceInfoResolver deviceInfoResolver;

    public SessionChallengeController(
            SessionChallengeService sessionChallengeService,
            AuthCookieService authCookieService,
            AuthenticatedSessionResolver authenticatedSessionResolver,
            DeviceInfoResolver deviceInfoResolver) {
        this.sessionChallengeService = sessionChallengeService;
        this.authCookieService = authCookieService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
        this.deviceInfoResolver = deviceInfoResolver;
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> listSessions(
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        List<SessionResponse> sessions = sessionChallengeService.getActiveSessionsForUser(userId);
        return ResponseEntity.ok(sessions);
    }

    @PostMapping("/claim-session")
    public ResponseEntity<AuthResponse> claimSession(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        DeviceInfo deviceInfo = deviceInfo(request);
        return authResponse(
                sessionChallengeService.claimSession(userId, deviceInfo),
                deviceInfo.clientType());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        sessionChallengeService.revokeSession(userId, sessionId);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AuthResponse> authResponse(AuthResponse response, ClientType clientType) {
        if (clientType == ClientType.WEB_BROWSER) {
            AuthResponse body = new AuthResponse(
                    response.accessToken(),
                    null,
                    response.expiresIn(),
                    response.sessionId());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, authCookieService.refreshCookie(response.refreshToken()).toString())
                    .body(body);
        }

        return ResponseEntity.ok(response);
    }

    private DeviceInfo deviceInfo(HttpServletRequest request) {
        return deviceInfoResolver.resolve(new ClientRequestMetadata(
                request.getHeader(HttpHeaders.USER_AGENT),
                request.getHeader("X-Client-Type"),
                request.getRemoteAddr()));
    }
}
