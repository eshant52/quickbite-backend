package com.quickbite.quickbite.controllers;

import com.quickbite.quickbite.dtos.auth.AuthResponse;
import com.quickbite.quickbite.dtos.auth.AuthenticatedSession;
import com.quickbite.quickbite.dtos.auth.ClaimSessionRequest;
import com.quickbite.quickbite.dtos.auth.DeviceInfo;
import com.quickbite.quickbite.dtos.auth.LoginRequest;
import com.quickbite.quickbite.dtos.auth.RefreshRequest;
import com.quickbite.quickbite.dtos.auth.RegisterRequest;
import com.quickbite.quickbite.dtos.auth.SessionResponse;
import com.quickbite.quickbite.dtos.UserResponseDto;
import com.quickbite.quickbite.exceptions.AuthenticationException;
import com.quickbite.quickbite.models.ClientType;
import com.quickbite.quickbite.models.RefreshToken;
import com.quickbite.quickbite.services.auth.*;
import com.quickbite.quickbite.utils.UserAgentParser;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final String SESSION_MANAGEMENT_TOKEN_HEADER = "X-Session-Management-Token";

    private final AuthService authService;
    private final SessionService sessionService;
    private final AuthCookieService authCookieService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;
    private final UserAgentParser userAgentParser;

    public AuthController(
            AuthService authService,
            SessionService sessionService,
            AuthCookieService authCookieService,
            AuthenticatedSessionResolver authenticatedSessionResolver,
            UserAgentParser userAgentParser) {
        this.authService = authService;
        this.sessionService = sessionService;
        this.authCookieService = authCookieService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
        this.userAgentParser = userAgentParser;
    }

    @PostMapping("/register/customer")
    public ResponseEntity<UserResponseDto> register(@RequestBody @Valid RegisterRequest registerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponseDto.toDto(authService.registerCustomer(registerRequest)));
    }

    @PostMapping("/register/delivery-partner")
    public ResponseEntity<UserResponseDto> deliveryPartner(@RequestBody @Valid RegisterRequest registerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponseDto.toDto(authService.registerDeliveryPartner(registerRequest)));
    }

    @PostMapping("/register/restaurant")
    public ResponseEntity<UserResponseDto> restaurant(@RequestBody @Valid RegisterRequest registerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponseDto.toDto(authService.registerRestaurant(registerRequest)));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest loginRequest,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
            @RequestHeader(value = "X-Client-Type", required = true) String clientTypeHeader) {
        DeviceInfo deviceInfo = userAgentParser.parse(userAgent, clientTypeHeader);
        return authResponse(authService.login(loginRequest, deviceInfo), deviceInfo.clientType());
    }

    @PostMapping("/claim-session")
    public ResponseEntity<AuthResponse> claimSession(
            @RequestBody @Valid ClaimSessionRequest claimSessionRequest,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
            @RequestHeader(value = "X-Client-Type", required = true) String clientTypeHeader) {
        DeviceInfo deviceInfo = userAgentParser.parse(userAgent, clientTypeHeader);
        return authResponse(
                authService.claimSession(claimSessionRequest.sessionManagementToken(), deviceInfo),
                deviceInfo.clientType());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestBody(required = false) RefreshRequest refreshRequest,
            @CookieValue(value = "${quickbite.auth.refresh-token-cookie-name:qb_refresh_token}", required = false)
            String cookieRefreshToken,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
            @RequestHeader(value = "X-Client-Type", required = true) String clientTypeHeader) {
        String rawRefreshToken = refreshRequest != null && refreshRequest.refreshToken() != null
                ? refreshRequest.refreshToken()
                : cookieRefreshToken;
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AuthenticationException("Refresh token is required");
        }

        DeviceInfo deviceInfo = userAgentParser.parse(userAgent, clientTypeHeader);
        ClientType responseType = cookieRefreshToken != null ? ClientType.WEB_BROWSER : deviceInfo.clientType();
        return authResponse(authService.refresh(rawRefreshToken), responseType);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        AuthenticatedSession session = authenticatedSessionResolver.currentSession(jwt);
        authService.logoutCurrentSession(session.userId(), session.familyId());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.expiredRefreshCookie().toString())
                .build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal Jwt jwt) {
        authService.logoutAllSessions(authenticatedSessionResolver.currentSession(jwt).userId());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.expiredRefreshCookie().toString())
                .build();
    }

    /**
     * Lists all active sessions/devices for the logged-in user.
     * Lets the user see "iPhone - Mumbai - 2 days ago" style entries
     * and pick one to revoke individually.
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> listSessions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = SESSION_MANAGEMENT_TOKEN_HEADER, required = false) String sessionManagementToken) {
        UUID userId = authenticatedSessionResolver.userIdFromJwtOrSessionManagementToken(jwt, sessionManagementToken);
        UUID currentFamilyId = jwt == null ? null : authenticatedSessionResolver.familyIdFromJwt(jwt);
        List<RefreshToken> sessions = sessionService.listActiveSessionsForUser(userId);

        List<SessionResponse> response = sessions.stream()
                .map(s -> new SessionResponse(
                        s.getFamilyId(),
                        s.getDeviceName(),
                        s.getOs(),
                        s.getClientType().name(),
                        s.getCreatedAt(),
                        s.getLastUsedAt(),
                        s.getFamilyId().equals(currentFamilyId)
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * "Log out this device" — revokes one specific session by family_id,
     * without touching the user's other active sessions.
     */
    @DeleteMapping("/sessions/{familyId}")
    public ResponseEntity<Void> revokeSession(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID familyId,
            @RequestHeader(value = SESSION_MANAGEMENT_TOKEN_HEADER, required = false) String sessionManagementToken) {
        UUID userId = authenticatedSessionResolver.userIdFromJwtOrSessionManagementToken(jwt, sessionManagementToken);
        sessionService.revokeSession(userId, familyId);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AuthResponse> authResponse(AuthResponse response, ClientType clientType) {
        if (clientType == ClientType.WEB_BROWSER) {
            AuthResponse body = new AuthResponse(
                    response.accessToken(),
                    null,
                    response.familyId(),
                    response.tokenType());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, authCookieService.refreshCookie(response.refreshToken()).toString())
                    .body(body);
        }
        return ResponseEntity.ok(response);
    }
}
