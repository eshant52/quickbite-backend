package com.quickbite.quickbite.auth.controller;

import com.quickbite.quickbite.auth.dto.AuthResponse;
import com.quickbite.quickbite.auth.dto.ClientRequestMetadata;
import com.quickbite.quickbite.auth.dto.DeviceInfo;
import com.quickbite.quickbite.auth.dto.LoginRequest;
import com.quickbite.quickbite.auth.dto.RefreshRequest;
import com.quickbite.quickbite.auth.exception.AuthenticationException;
import com.quickbite.quickbite.auth.model.ClientType;
import com.quickbite.quickbite.auth.service.AuthCookieService;
import com.quickbite.quickbite.auth.service.AuthenticationService;
import com.quickbite.quickbite.auth.util.DeviceInfoResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final AuthCookieService authCookieService;
    private final DeviceInfoResolver deviceInfoResolver;

    public AuthenticationController(
            AuthenticationService authenticationService,
            AuthCookieService authCookieService,
            DeviceInfoResolver deviceInfoResolver) {
        this.authenticationService = authenticationService;
        this.authCookieService = authCookieService;
        this.deviceInfoResolver = deviceInfoResolver;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest loginRequest,
            HttpServletRequest request) {
        DeviceInfo deviceInfo = deviceInfo(request);
        return authResponse(authenticationService.login(loginRequest, deviceInfo), deviceInfo.clientType());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestBody(required = false) RefreshRequest refreshRequest,
            HttpServletRequest request) {
        DeviceInfo deviceInfo = deviceInfo(request);

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

        return authResponse(authenticationService.refresh(rawRefreshToken), deviceInfo.clientType());
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
