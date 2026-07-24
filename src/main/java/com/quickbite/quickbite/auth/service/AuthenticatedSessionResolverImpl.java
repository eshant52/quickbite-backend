package com.quickbite.quickbite.auth.service;

import com.quickbite.quickbite.auth.dto.AuthenticatedSession;
import com.quickbite.quickbite.auth.exception.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class AuthenticatedSessionResolverImpl implements AuthenticatedSessionResolver {
    public AuthenticatedSessionResolverImpl() {}

    @Override
    public UUID userIdFromJwt(Jwt jwt) {
        if (Objects.isNull(jwt) || Objects.isNull(jwt.getSubject()) || jwt.getSubject().isBlank()) {
            throw new AuthenticationException("Invalid token");
        }
        return UUID.fromString(jwt.getSubject());
    }

    @Override
    public UUID sessionIdFromJwt(Jwt jwt) {
        if (Objects.isNull(jwt) || Objects.isNull(jwt.getSubject()) || jwt.getSubject().isBlank()) {
            throw new AuthenticationException("Invalid token");
        }

        String rawSessionId = jwt.getClaimAsString("session_id");
        if (rawSessionId == null || rawSessionId.isBlank()) {
            throw new AuthenticationException("Invalid token: missing session claim");
        }

        return UUID.fromString(rawSessionId);
    }

    @Override
    public AuthenticatedSession currentSession(Jwt jwt) {
        if (Objects.isNull(jwt) || Objects.isNull(jwt.getSubject()) || jwt.getSubject().isBlank()) {
            throw new AuthenticationException("Invalid token");
        }

        UUID userId = UUID.fromString(jwt.getSubject());
        UUID sessionId = sessionIdFromJwt(jwt);
        return new AuthenticatedSession(userId, sessionId);
    }
}
