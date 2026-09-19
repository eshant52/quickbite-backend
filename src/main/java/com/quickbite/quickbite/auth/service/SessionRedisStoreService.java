package com.quickbite.quickbite.auth.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class SessionRedisStoreService implements SessionStoreService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String GRACE_TOKEN_PREFIX = "quickbite:token-grace:";

    public SessionRedisStoreService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void cacheRotatedTokenGrace(String oldTokenHash, String newRawToken, Duration ttl) {
        redisTemplate.opsForValue().set(graceKey(oldTokenHash), newRawToken, ttl);
    }

    @Override
    public Optional<String> getRotatedTokenGrace(String oldTokenHash) {
        Object token = redisTemplate.opsForValue().get(graceKey(oldTokenHash));
        return token != null ? Optional.of(token.toString()) : Optional.empty();
    }

    private String graceKey(String oldTokenHash) {
        return GRACE_TOKEN_PREFIX + oldTokenHash;
    }
}
