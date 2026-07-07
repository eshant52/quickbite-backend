package com.quickbite.quickbite.services.auth;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SessionRedisStoreService implements SessionStoreService {
    private final RedisTemplate<String, Object> redisTemplate;

    private final static String KEY_PREFIX = "quickbite:session:";
    private final static String LOCK_PREFIX = "quickbite:session-lock:";

    public SessionRedisStoreService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void addSession(UUID userId, UUID familyId, Duration expiration) {
        redisTemplate.opsForSet().add(userKey(userId), familyId.toString());
        redisTemplate.expire(userKey(userId), expiration);
    }

    @Override
    public void removeSession(UUID userId, UUID familyId) {
        redisTemplate.opsForSet().remove(userKey(userId), familyId.toString());
    }

    @Override
    public void removeAllSessions(UUID userId) {
        redisTemplate.delete(userKey(userId));
    }

    @Override
    public Set<UUID> getActiveSessions(UUID userId) {
        Set<Object> activeSessions = redisTemplate.opsForSet().members(userKey(userId));
        if (activeSessions == null || activeSessions.isEmpty())
            return Set.of();
        return activeSessions.stream()
                .map(Object::toString)
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }

    @Override
    public long getActiveSessionsCount(UUID userId) {
        Long activeSessionCount = redisTemplate.opsForSet().size(userKey(userId));
        return activeSessionCount == null ? 0L : activeSessionCount;
    }

    @Override
    public boolean acquireSessionCreationLock(UUID userId, Duration ttl) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey(userId), "1", ttl);
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void releaseSessionCreationLock(UUID userId) {
        redisTemplate.delete(lockKey(userId));
    }

    private String userKey(UUID userId) {
        return KEY_PREFIX + userId.toString();
    }

    private String lockKey(UUID userId) {
        return LOCK_PREFIX + userId.toString();
    }
}
