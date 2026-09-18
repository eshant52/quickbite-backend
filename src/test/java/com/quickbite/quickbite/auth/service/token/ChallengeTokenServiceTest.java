package com.quickbite.quickbite.auth.service.token;

import com.quickbite.quickbite.auth.exception.AuthenticationException;
import com.quickbite.quickbite.common.config.property.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeTokenServiceTest {

    @Mock
    private TokenService tokenService;

    private AuthProperties authProperties;
    private ChallengeTokenService challengeTokenService;

    @BeforeEach
    void setUp() {
        // Assumption in tests: JWT configuration exists in environment
        AuthProperties.JwtProperties jwtProperties = new AuthProperties.JwtProperties(
                "classpath:certs/private.pem",
                "classpath:certs/public.pem",
                Duration.ofMinutes(15),
                "quickbite-api",
                Duration.ofDays(7),
                Duration.ofMinutes(5),
                "quickbite-auth"
        );
        authProperties = new AuthProperties(3, null, jwtProperties, null);
        challengeTokenService = new ChallengeTokenService(authProperties, tokenService);
    }

    @Test
    @DisplayName("Crashes on startup if JWT configuration is missing in environment (no null checking)")
    void constructor_crashes_whenJwtConfigurationMissing() {
        AuthProperties missingJwtProps = new AuthProperties(3, null, null, null);
        assertThatThrownBy(() -> new ChallengeTokenService(missingJwtProps, tokenService))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Successfully generates challenge token with explicit expiry")
    void generateSessionLimitChallenge_withExplicitExpiry() {
        UUID userId = UUID.randomUUID();
        Instant expiryAt = Instant.now().plusSeconds(300);
        String expectedJwt = "mocked.challenge.jwt";

        when(tokenService.generateToken(any(), any(), any(), any())).thenReturn(expectedJwt);

        String token = challengeTokenService.generateSessionLimitChallenge(userId, expiryAt);

        assertThat(token).isEqualTo(expectedJwt);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);

        verify(tokenService).generateToken(
                eq(userId.toString()),
                eq(List.of("quickbite-auth")),
                claimsCaptor.capture(),
                eq(expiryAt)
        );

        Map<String, Object> claims = claimsCaptor.getValue();
        assertThat(claims).containsEntry("type", "session-limit");
    }

    @Test
    @DisplayName("Successfully generates challenge token with default 5-minute expiry")
    void generateSessionLimitChallenge_withDefaultExpiry() {
        UUID userId = UUID.randomUUID();
        String expectedJwt = "mocked.challenge.jwt";

        when(tokenService.generateToken(any(), any(), any(), any())).thenReturn(expectedJwt);

        Instant before = Instant.now().plus(Duration.ofMinutes(5));
        String token = challengeTokenService.generateSessionLimitChallenge(userId);
        Instant after = Instant.now().plus(Duration.ofMinutes(5));

        assertThat(token).isEqualTo(expectedJwt);

        ArgumentCaptor<Instant> expiryCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(tokenService).generateToken(eq(userId.toString()), eq(List.of("quickbite-auth")), any(), expiryCaptor.capture());

        assertThat(expiryCaptor.getValue()).isBetween(before.minusSeconds(2), after.plusSeconds(2));
    }

    @Test
    @DisplayName("Verifies valid challenge token and returns user ID")
    void verifySessionLimitChallenge_success() {
        UUID userId = UUID.randomUUID();
        String token = "valid.challenge.jwt";

        when(tokenService.parseAndVerifyToken(token)).thenReturn(Map.of(
                "sub", userId.toString(),
                "type", "session-limit"
        ));

        UUID result = challengeTokenService.verifySessionLimitChallenge(token);

        assertThat(result).isEqualTo(userId);
    }

    @Test
    @DisplayName("Throws AuthenticationException when challenge token has invalid type claim")
    void verifySessionLimitChallenge_invalidTypeClaim() {
        String token = "invalid.challenge.jwt";

        when(tokenService.parseAndVerifyToken(token)).thenReturn(Map.of(
                "sub", UUID.randomUUID().toString(),
                "type", "wrong-type"
        ));

        assertThatThrownBy(() -> challengeTokenService.verifySessionLimitChallenge(token))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid challenge token");
    }

    @Test
    @DisplayName("Returns default expiration duration in seconds (300s / 5min)")
    void defaultExpirationDurationInSeconds() {
        assertThat(challengeTokenService.defaultExpirationDurationInSeconds()).isEqualTo(300L);
    }
}
