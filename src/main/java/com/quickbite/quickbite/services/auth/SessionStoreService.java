package com.quickbite.quickbite.services.auth;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

public interface SessionStoreService {
    void addSession(UUID userId, UUID familyId, Duration expiration);
    void removeSession(UUID userId, UUID familyId);
    void removeAllSessions(UUID userId);
    Set<UUID> getActiveSessions(UUID userId);
    long getActiveSessionsCount(UUID userId);
    boolean acquireSessionCreationLock(UUID userId, Duration ttl);
    void releaseSessionCreationLock(UUID userId);
}
