package com.quickbite.quickbite.services;

import com.quickbite.quickbite.dtos.auth.DeviceInfo;
import com.quickbite.quickbite.dtos.auth.IssuedToken;
import com.quickbite.quickbite.models.RefreshToken;
import com.quickbite.quickbite.models.User;

import java.util.List;
import java.util.UUID;

public interface SessionService {
    IssuedToken createNewSession(User user, DeviceInfo deviceInfo);
    IssuedToken validateAndRotate(String rawToken);
    void revokeSession(UUID userId, UUID familyId);
    void revokeAllSessions(UUID userId);
    List<RefreshToken> listActiveSessionsForUser(UUID userId);
}
