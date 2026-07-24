package com.quickbite.quickbite.auth.service;

import com.quickbite.quickbite.auth.dto.DeviceInfo;
import com.quickbite.quickbite.auth.dto.IssuedToken;
import com.quickbite.quickbite.auth.dto.SessionResponse;
import com.quickbite.quickbite.auth.model.Session;
import com.quickbite.quickbite.user.model.User;

import java.util.List;
import java.util.UUID;

public interface SessionService {
    IssuedToken createNewSession(User user, DeviceInfo deviceInfo);

    IssuedToken validateAndRotate(String rawToken);

    void revokeSession(UUID userId, UUID sessionId);

    void revokeAllSessions(UUID userId);

    List<SessionResponse> listActiveSessionsForUser(UUID userId);
}
