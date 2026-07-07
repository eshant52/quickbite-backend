package com.quickbite.quickbite.services.auth;

import com.quickbite.quickbite.dtos.auth.AuthenticatedSession;
import com.quickbite.quickbite.exceptions.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class AuthenticatedSessionResolverImpl implements AuthenticatedSessionResolver {
    private final SessionManagementTokenStoreService sessionManagementTokenStoreService;

    public AuthenticatedSessionResolverImpl(SessionManagementTokenStoreService sessionManagementTokenStoreService) {
        this.sessionManagementTokenStoreService = sessionManagementTokenStoreService;
    }

    public UUID userIdFromJwtOrSessionManagementToken(Jwt jwt, String sessionManagementToken) {
        if (sessionManagementToken != null && !sessionManagementToken.isBlank()) {
            return sessionManagementTokenStoreService.validateAndGetUserId(sessionManagementToken);
        }
        return currentSession(jwt).userId();
    }

    public AuthenticatedSession currentSession(Jwt jwt) {
        if (jwt == null) {
            throw new AuthenticationException("Authentication is required");
        }
        return new AuthenticatedSession(UUID.fromString(Objects.requireNonNull(jwt.getSubject())), familyIdFromJwt(jwt));
    }

    public UUID familyIdFromJwt(Jwt jwt) {
        String familyId = jwt.getClaimAsString("family_id");
        if (familyId == null || familyId.isBlank()) {
            throw new AuthenticationException("Access token does not contain a session family");
        }
        return UUID.fromString(familyId);
    }
}
