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
class AccessTokenServiceTest {

    @Mock
    private TokenService tokenService;

    private AuthProperties authProperties;
    private AccessTokenService accessTokenService;

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
        accessTokenService = new AccessTokenService(authProperties, tokenService);
    }

    @Test
    @DisplayName("Crashes on startup if JWT configuration is missing in environment (no null checking)")
    void constructor_crashes_whenJwtConfigurationMissing() {
        AuthProperties missingJwtProps = new AuthProperties(3, null, null, null);
        assertThatThrownBy(() -> new AccessTokenService(missingJwtProps, tokenService))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Successfully generates access token with explicit expiry")
    void generateAccessToken_withExplicitExpiry() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant expiryAt = Instant.now().plusSeconds(600);
        String expectedJwt = "mocked.access.jwt";

        when(tokenService.generateToken(any(), any(), any(), any())).thenReturn(expectedJwt);

        String token = accessTokenService.generateAccessToken(userId, "user@example.com", "ROLE_CUSTOMER", sessionId, expiryAt);

        assertThat(token).isEqualTo(expectedJwt);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);

        verify(tokenService).generateToken(
                eq(userId.toString()),
                eq(List.of("quickbite-api")),
                claimsCaptor.capture(),
                eq(expiryAt)
        );

        Map<String, Object> claims = claimsCaptor.getValue();
        assertThat(claims).containsEntry("session_id", sessionId.toString());
        assertThat(claims).containsEntry("email", "user@example.com");
        assertThat(claims).containsEntry("role", "ROLE_CUSTOMER");
        assertThat(claims).containsEntry("type", "access");
    }

    @Test
    @DisplayName("Successfully generates access token with default 15-minute expiry")
    void generateAccessToken_withDefaultExpiry() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String expectedJwt = "mocked.access.jwt";

        when(tokenService.generateToken(any(), any(), any(), any())).thenReturn(expectedJwt);

        Instant before = Instant.now().plus(Duration.ofMinutes(15));
        String token = accessTokenService.generateAccessToken(userId, "user@example.com", "ROLE_CUSTOMER", sessionId);
        Instant after = Instant.now().plus(Duration.ofMinutes(15));

        assertThat(token).isEqualTo(expectedJwt);

        ArgumentCaptor<Instant> expiryCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(tokenService).generateToken(eq(userId.toString()), eq(List.of("quickbite-api")), any(), expiryCaptor.capture());

        assertThat(expiryCaptor.getValue()).isBetween(before.minusSeconds(2), after.plusSeconds(2));
    }

    @Test
    @DisplayName("Verifies valid access token and returns user ID")
    void verifyAccessToken_success() {
        UUID userId = UUID.randomUUID();
        String token = "valid.jwt.token";

        when(tokenService.parseAndVerifyToken(token)).thenReturn(Map.of(
                "sub", userId.toString(),
                "type", "access"
        ));

        UUID result = accessTokenService.verifyAccessToken(token);

        assertThat(result).isEqualTo(userId);
    }

    @Test
    @DisplayName("Throws AuthenticationException when access token has invalid type claim")
    void verifyAccessToken_invalidTypeClaim() {
        String token = "invalid.jwt.token";

        when(tokenService.parseAndVerifyToken(token)).thenReturn(Map.of(
                "sub", UUID.randomUUID().toString(),
                "type", "wrong-type"
        ));

        assertThatThrownBy(() -> accessTokenService.verifyAccessToken(token))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid access token");
    }

    @Test
    @DisplayName("Returns default expiration duration in seconds (900s / 15min)")
    void defaultExpirationDurationInSeconds() {
        assertThat(accessTokenService.defaultExpirationDurationInSeconds()).isEqualTo(900L);
    }
}
