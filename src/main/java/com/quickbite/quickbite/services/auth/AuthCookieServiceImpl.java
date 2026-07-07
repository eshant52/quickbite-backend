package com.quickbite.quickbite.services.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthCookieServiceImpl implements AuthCookieService {
    private final String refreshTokenCookieName;
    private final String refreshTokenCookiePath;
    private final Duration refreshTokenExpiry;

    public AuthCookieServiceImpl(
            @Value("${quickbite.auth.refresh-token-cookie-name:qb_refresh_token}") String refreshTokenCookieName,
            @Value("${quickbite.auth.refresh-token-cookie-path:/api/v1/auth}") String refreshTokenCookiePath,
            @Value("${quickbite.jwt.refresh-token-expiry:P7D}") String refreshTokenExpiry) {
        this.refreshTokenCookieName = refreshTokenCookieName;
        this.refreshTokenCookiePath = refreshTokenCookiePath;
        this.refreshTokenExpiry = Duration.parse(refreshTokenExpiry);
    }

    public ResponseCookie refreshCookie(String refreshToken) {
        return baseCookie(refreshToken)
                .maxAge(refreshTokenExpiry)
                .build();
    }

    public ResponseCookie expiredRefreshCookie() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(refreshTokenCookieName, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(refreshTokenCookiePath);
    }
}
