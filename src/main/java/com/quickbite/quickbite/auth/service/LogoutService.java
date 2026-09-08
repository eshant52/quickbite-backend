package com.quickbite.quickbite.auth.service;

import java.util.UUID;

/**
 * Service dedicated to terminating active authenticated sessions (SCOPE_API clearance).
 */
public interface LogoutService {

    void logoutCurrentSession(UUID userId, UUID sessionId);

    void logoutAllSessions(UUID userId);
}
