package com.quickbite.quickbite.auth.service.token;

import com.quickbite.quickbite.auth.exception.AuthenticationException;
import com.quickbite.quickbite.common.config.property.AuthProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChallengeTokenService {

    private final Duration expiryDuration;
    private final String audience;

    private static final String TYPE_CLAIM = "type";
    private static final String TYPE_VALUE = "session-limit";

    private final TokenService tokenService;

    public ChallengeTokenService(AuthProperties authProperties, TokenService tokenService) {
        this.expiryDuration = authProperties.jwt().challengeTokenExpiry();
        this.audience = authProperties.jwt().challengeTokenAudience();
        this.tokenService = tokenService;
    }

    /**
     * Generates a session limit challenge token for the given user ID with a specified expiry time.
     * @param userId The UUID of the user for whom the challenge token is being generated.
     * @param expiryAt The instant at which the challenge token will expire.
     * @return A JWT challenge token as a String.
     * @see ChallengeTokenService#generateSessionLimitChallenge(UUID)
     */
    public String generateSessionLimitChallenge(UUID userId, Instant expiryAt) {
        return tokenService.generateToken(
                userId.toString(),
                List.of(audience),
                Map.of(
                        TYPE_CLAIM, TYPE_VALUE
                ),
                expiryAt
        );
    }

    /**
     * Generates a session limit challenge token for the given user ID with a default expiry time.
     * Default expiry duration is 5 minutes from the current time.
     * @param userId The UUID of the user for whom the challenge token is being generated.
     * @return A JWT challenge token as a String.
     * @see ChallengeTokenService#generateSessionLimitChallenge(UUID, Instant)
     */
    public String generateSessionLimitChallenge(UUID userId) {
        return generateSessionLimitChallenge(userId, Instant.now().plus(expiryDuration));
    }


    public UUID verifySessionLimitChallenge(String token) {
        Map<String, Object> claims = tokenService.parseAndVerifyToken(token);
        if (!TYPE_VALUE.equals(claims.get(TYPE_CLAIM))) {
            throw new AuthenticationException("Invalid challenge token");
        }
        return UUID.fromString((String) claims.get("sub"));
    }

    public Long defaultExpirationDurationInSeconds() {
        return expiryDuration.getSeconds();
    }
}
