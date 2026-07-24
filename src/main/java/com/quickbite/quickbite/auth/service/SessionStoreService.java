package com.quickbite.quickbite.auth.service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

public interface SessionStoreService {
    boolean addSession(UUID userId, UUID sessionId, Duration expiration);

    boolean removeSession(UUID userId, UUID sessionId);

    boolean removeAllSessions(UUID userId);

    Set<UUID> getActiveSessions(UUID userId);

    long getActiveSessionsCount(UUID userId);

    boolean acquireSessionCreationLock(UUID userId, Duration ttl);

    boolean releaseSessionCreationLock(UUID userId);
}
