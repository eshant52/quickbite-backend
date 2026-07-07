package com.quickbite.quickbite.services.auth;

import com.quickbite.quickbite.dtos.auth.AuthenticatedSession;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public interface AuthenticatedSessionResolver {
    UUID userIdFromJwtOrSessionManagementToken(Jwt jwt, String sessionManagementToken);
    AuthenticatedSession currentSession(Jwt jwt);
    UUID familyIdFromJwt(Jwt jwt);
}
