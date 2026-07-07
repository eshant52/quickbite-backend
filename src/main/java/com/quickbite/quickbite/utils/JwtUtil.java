package com.quickbite.quickbite.utils;

import com.quickbite.quickbite.models.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class JwtUtil {
    private final JwtEncoder jwtEncoder;

    @Value("${quickbite.jwt.access-token-expiry}")
    private String accessTokenExpiry;

    public JwtUtil(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String generateAccessToken(User user, UUID familyId) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .audience(List.of("user"))
                .issuer("quickbite")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(Duration.parse(accessTokenExpiry).getSeconds()))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .claim("family_id", familyId.toString())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
