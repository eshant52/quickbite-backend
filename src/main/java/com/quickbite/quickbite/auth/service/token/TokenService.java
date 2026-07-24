package com.quickbite.quickbite.auth.service.token;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface TokenService {
    String generateToken(String subject, List<String> audience, Map<String, Object> claims, Instant expiryAt);
    Map<String, Object> parseAndVerifyToken(String token);
}
