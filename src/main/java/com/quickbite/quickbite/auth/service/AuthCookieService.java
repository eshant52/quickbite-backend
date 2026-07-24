package com.quickbite.quickbite.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

public interface AuthCookieService {
    ResponseCookie refreshCookie(String refreshToken);
    ResponseCookie expiredRefreshCookie();
    String extractRefreshTokenFromCookie(HttpServletRequest request);
}
