package com.quickbite.quickbite.auth.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SessionRedisStoreService implements SessionStoreService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "quickbite:session:";
    private static final String LOCK_PREFIX = "quickbite:session-lock:";

    public SessionRedisStoreService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean addSession(UUID userId, UUID sessionId, Duration expiration) {
        String key = userKey(userId);

        // Returns 1 if added, 0 if already exists
        Long addedCount = redisTemplate.opsForSet().add(key, sessionId.toString());
        boolean isAdded = addedCount != null && addedCount > 0;

        if (isAdded) {
            redisTemplate.expire(key, expiration);
        }

        return isAdded;
    }

    @Override
    public boolean removeSession(UUID userId, UUID sessionId) {
        // Returns 1 if removed, 0 if it did not exist
        Long removedCount = redisTemplate.opsForSet().remove(userKey(userId), sessionId.toString());
        return removedCount != null && removedCount > 0;
    }

    @Override
    public boolean removeAllSessions(UUID userId) {
        Boolean deleted = redisTemplate.delete(userKey(userId));
        // Safely handles unboxing to prevent NullPointerException
        return Boolean.TRUE.equals(deleted);
    }

    @Override
    public Set<UUID> getActiveSessions(UUID userId) {
        Set<Object> activeSessions = redisTemplate.opsForSet().members(userKey(userId));
        if (activeSessions == null || activeSessions.isEmpty()) {
            return Set.of();
        }

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
    public boolean releaseSessionCreationLock(UUID userId) {
        Boolean released = redisTemplate.delete(lockKey(userId));
        return Boolean.TRUE.equals(released);
    }

    private String userKey(UUID userId) {
        return KEY_PREFIX + userId.toString();
    }

    private String lockKey(UUID userId) {
        return LOCK_PREFIX + userId.toString();
    }
}
