package com.quickbite.quickbite.auth.service;

import java.time.Duration;
import java.util.Optional;

public interface SessionStoreService {

    void cacheRotatedTokenGrace(String oldTokenHash, String newRawToken, Duration ttl);

    Optional<String> getRotatedTokenGrace(String oldTokenHash);
}
