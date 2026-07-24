package com.quickbite.quickbite.auth.service;

import com.quickbite.quickbite.auth.dto.AuthenticatedSession;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public interface AuthenticatedSessionResolver {
    UUID userIdFromJwt(Jwt jwt);

    UUID sessionIdFromJwt(Jwt jwt);

    AuthenticatedSession currentSession(Jwt jwt);
}
