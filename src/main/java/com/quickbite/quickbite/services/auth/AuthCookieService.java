package com.quickbite.quickbite.services.auth;

import org.springframework.http.ResponseCookie;

public interface AuthCookieService {
    ResponseCookie refreshCookie(String refreshToken);
    ResponseCookie expiredRefreshCookie();
}
