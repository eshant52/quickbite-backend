package com.quickbite.quickbite.auth.service;

import com.quickbite.quickbite.common.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

@Service
public class AuthCookieServiceImpl implements AuthCookieService {
    private final AuthProperties authProperties;

    public AuthCookieServiceImpl(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public ResponseCookie refreshCookie(String refreshToken) {
        return baseCookie(refreshToken)
                .maxAge(authProperties.jwt().refreshTokenExpiry().getSeconds())
                .build();
    }

    public ResponseCookie expiredRefreshCookie() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    @Override
    public String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, authProperties.cookie().refreshTokenName());
        return cookie != null ? cookie.getValue() : null;
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(authProperties.cookie().refreshTokenName(), value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(authProperties.cookie().refreshTokenPath());
    }
}
