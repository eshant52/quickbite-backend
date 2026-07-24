package com.quickbite.quickbite.auth.controller;

import com.quickbite.quickbite.auth.dto.AuthResponse;
import com.quickbite.quickbite.auth.dto.AuthenticatedSession;
import com.quickbite.quickbite.auth.dto.DeviceInfo;
import com.quickbite.quickbite.auth.dto.LoginRequest;
import com.quickbite.quickbite.auth.dto.RefreshRequest;
import com.quickbite.quickbite.auth.dto.RegisterRequest;
import com.quickbite.quickbite.auth.dto.SessionResponse;
import com.quickbite.quickbite.auth.exception.AuthenticationException;
import com.quickbite.quickbite.auth.model.ClientType;
import com.quickbite.quickbite.auth.service.AuthCookieService;
import com.quickbite.quickbite.auth.service.AuthService;
import com.quickbite.quickbite.auth.service.AuthenticatedSessionResolver;
import com.quickbite.quickbite.auth.util.UserAgentParser;
import com.quickbite.quickbite.user.dto.UserResponseDto;
import jakarta.servlet.http.HttpServletRequest;
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

    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final AuthenticatedSessionResolver authenticatedSessionResolver;
    private final UserAgentParser userAgentParser;


    public AuthController(
            AuthService authService,
            AuthCookieService authCookieService,
            AuthenticatedSessionResolver authenticatedSessionResolver,
            UserAgentParser userAgentParser) {
        this.authService = authService;
        this.authCookieService = authCookieService;
        this.authenticatedSessionResolver = authenticatedSessionResolver;
        this.userAgentParser = userAgentParser;
    }


    @PostMapping("/register/customer")
    public ResponseEntity<UserResponseDto> register(@RequestBody @Valid RegisterRequest registerRequest) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerCustomer(RegisterRequest.xssValidate(registerRequest)));
    }


    @PostMapping("/register/delivery-partner")
    public ResponseEntity<UserResponseDto> deliveryPartner(@RequestBody @Valid RegisterRequest registerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerDeliveryPartner(RegisterRequest.xssValidate(registerRequest)));
    }


    @PostMapping("/register/restaurant")
    public ResponseEntity<UserResponseDto> restaurant(@RequestBody @Valid RegisterRequest registerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerRestaurant(RegisterRequest.xssValidate(registerRequest)));
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest loginRequest,
            HttpServletRequest request) {
        DeviceInfo deviceInfo = userAgentParser.parse(request);
        return authResponse(authService.login(loginRequest, deviceInfo), deviceInfo.clientType());
    }


    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> listSessions(
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        List<SessionResponse> sessions = authService.getActiveSessionsForUser(userId);
        return ResponseEntity.ok(sessions);
    }


    @PostMapping("/claim-session")
    public ResponseEntity<AuthResponse> claimSession(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        DeviceInfo deviceInfo = userAgentParser.parse(request);
        return authResponse(
                authService.claimSession(userId, deviceInfo),
                deviceInfo.clientType());
    }


    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId) {
        UUID userId = authenticatedSessionResolver.userIdFromJwt(jwt);
        authService.revokeSession(userId, sessionId);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestBody(required = false) RefreshRequest refreshRequest,
            HttpServletRequest request) {
        DeviceInfo deviceInfo = userAgentParser.parse(request);

        String rawRefreshToken = switch (deviceInfo.clientType()) {
            case MOBILE_APP -> {
                if (refreshRequest == null || refreshRequest.refreshToken() == null || refreshRequest.refreshToken().isBlank()) {
                    throw new AuthenticationException("Refresh token is required in the request body for mobile app clients");
                }
                yield refreshRequest.refreshToken();
            }
            case WEB_BROWSER -> {
                String token = authCookieService.extractRefreshTokenFromCookie(request);
                if (token == null || token.isBlank()) {
                    throw new AuthenticationException("Refresh token is required in the cookie for web browser clients");
                }
                yield token;
            }
        };

        if (rawRefreshToken.isBlank()) {
            throw new AuthenticationException("Refresh token is required");
        }

        return authResponse(authService.refresh(rawRefreshToken), deviceInfo.clientType());
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        AuthenticatedSession session = authenticatedSessionResolver.currentSession(jwt);
        authService.logoutCurrentSession(session.userId(), session.sessionId());
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
}
