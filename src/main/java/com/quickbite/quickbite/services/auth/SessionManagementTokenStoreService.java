package com.quickbite.quickbite.services.auth;

import java.util.UUID;

public interface SessionManagementTokenStoreService {
    String createToken(UUID userId);
    UUID validateAndGetUserId(String rawToken);
    void invalidate(String rawToken);
}
