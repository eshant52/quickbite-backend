package com.quickbite.quickbite.auth.service.token;

import com.quickbite.quickbite.auth.exception.AuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AccessTokenService {
    private final Duration expiryDuration;
    private final String audience;

    private static final String TYPE_CLAIM = "type";
    private static final String TYPE_VALUE = "access";

    private final TokenService tokenService;


    public AccessTokenService(
            @Value("${quickbite.jwt.access-token-expiry:PT15M}") Duration expiryDuration,
            @Value("${quickbite.jwt.access-token-audience:quickbite-api}") String audience,
            TokenService tokenService) {
        this.expiryDuration = expiryDuration;
        this.audience = audience;
        this.tokenService = tokenService;
    }


    /**
     * Generate an access token for the given user ID, email, role, and session ID with a specified expiry time.
     *
     * @param userId    The UUID of the user for whom the access token is being generated.
     * @param email     The email of the user.
     * @param role      The role of the user.
     * @param sessionId The UUID of the session.
     * @param expiryAt  The instant at which the access token will expire.
     * @return A JWT access token as a String.
     */
    public String generateAccessToken(UUID userId, String email, String role, UUID sessionId, Instant expiryAt) {
        return tokenService.generateToken(
                userId.toString(),
                List.of(audience),
                Map.of(
                        "session_id", sessionId.toString(),
                        "email", email,
                        "role", role,
                        TYPE_CLAIM, TYPE_VALUE
                ),
                expiryAt
        );
    }


    /**
     * Generate an access token for the given user ID, email, role, and session ID with a default expiry time.
     * Default expiry duration is 15 minutes from the current time.
     *
     * @param userId    The UUID of the user for whom the access token is being generated.
     * @param email     The email of the user.
     * @param role      The role of the user.
     * @param sessionId The UUID of the session.
     * @return A JWT access token as a String.
     */
    public String generateAccessToken(UUID userId, String email, String role, UUID sessionId) {
        return generateAccessToken(userId, email, role, sessionId, Instant.now().plus(expiryDuration));
    }


    public UUID verifyAccessToken(String token) {
        Map<String, Object> claims = tokenService.parseAndVerifyToken(token);
        if (!TYPE_VALUE.equals(claims.get(TYPE_CLAIM))) {
            throw new AuthenticationException("Invalid access token");
        }
        return UUID.fromString((String) claims.get("sub"));
    }


    public Long defaultExpirationDurationInSeconds() {
        return expiryDuration.getSeconds();
    }
}
