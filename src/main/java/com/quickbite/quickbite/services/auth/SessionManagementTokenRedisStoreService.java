package com.quickbite.quickbite.services.auth;

import com.quickbite.quickbite.exceptions.AuthenticationException;
import com.quickbite.quickbite.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class SessionManagementTokenRedisStoreService implements SessionManagementTokenStoreService {
    private static final String KEY_PREFIX = "quickbite:session-mgmt:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${quickbite.auth.session-management-token-ttl:PT5M}")
    private String tokenTtl;

    public SessionManagementTokenRedisStoreService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String createToken(UUID userId) {
        String rawToken = TokenUtils.generateOpaqueToken();
        redisTemplate.opsForValue().set(key(rawToken), userId.toString(), Duration.parse(tokenTtl));
        return rawToken;
    }

    public UUID validateAndGetUserId(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new AuthenticationException("Session management token is required");
        }

        Object storedUserId = redisTemplate.opsForValue().get(key(rawToken));
        if (storedUserId == null) {
            throw new AuthenticationException("Invalid or expired session management token");
        }
        return UUID.fromString(storedUserId.toString());
    }

    public void invalidate(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) {
            redisTemplate.delete(key(rawToken));
        }
    }

    private String key(String rawToken) {
        return KEY_PREFIX + TokenUtils.sha256(rawToken);
    }
}
