package com.quickbite.quickbite.auth.service;

import com.quickbite.quickbite.auth.dto.AuthResponse;
import com.quickbite.quickbite.auth.dto.DeviceInfo;
import com.quickbite.quickbite.auth.dto.SessionResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service dedicated to resolving concurrent session challenges (SCOPE_AUTH token clearance).
 */
public interface SessionChallengeService {

    List<SessionResponse> getActiveSessionsForUser(UUID userId);

    void revokeSession(UUID userId, UUID sessionId);

    AuthResponse claimSession(UUID userId, DeviceInfo deviceInfo);
}
