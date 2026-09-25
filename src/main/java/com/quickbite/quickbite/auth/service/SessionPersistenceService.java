package com.quickbite.quickbite.auth.service;

import com.quickbite.quickbite.auth.dto.DeviceInfo;
import com.quickbite.quickbite.auth.model.RefreshTokenFamily;
import com.quickbite.quickbite.auth.model.Session;
import com.quickbite.quickbite.user.model.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SessionPersistenceService {

    Session persistNewSession(User user, DeviceInfo deviceInfo, Instant expiresAt, String rawToken);

    boolean markTokenUsed(UUID tokenId, Instant now);

    void saveRotatedToken(RefreshTokenFamily family, Session session, String newRawToken, int nextGeneration, Instant expiresAt);

    void revokeBreachedFamily(UUID familyId, UUID sessionId, UUID userId);

    int revokeSession(UUID userId, UUID sessionId);

    int revokeAllSessions(UUID userId);

    List<Session> findActiveSessionsByUserId(UUID userId);

    int purgeStaleTokens(Instant now, Instant cutoff);
}
