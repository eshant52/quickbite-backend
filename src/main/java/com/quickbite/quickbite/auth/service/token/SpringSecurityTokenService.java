package com.quickbite.quickbite.auth.service.token;

import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class SpringSecurityTokenService implements TokenService {
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public SpringSecurityTokenService(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public String generateToken(String subject, List<String> audience, Map<String, Object> claims, Instant expiryAt) {
        JwtClaimsSet.Builder claimsSetBuilder = JwtClaimsSet.builder()
                .id(java.util.UUID.randomUUID().toString())
                .audience(audience)
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(expiryAt);

        claims.forEach(claimsSetBuilder::claim);

        JwtEncoderParameters parameters = JwtEncoderParameters.from(claimsSetBuilder.build());

        return jwtEncoder.encode(parameters).getTokenValue();
    }

    @Override
    public Map<String, Object> parseAndVerifyToken(String token) {
        Jwt jwt = this.jwtDecoder.decode(token);
        return jwt.getClaims();
    }
}
