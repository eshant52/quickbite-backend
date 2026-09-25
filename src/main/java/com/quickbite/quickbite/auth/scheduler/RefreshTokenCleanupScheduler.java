package com.quickbite.quickbite.auth.scheduler;

import com.quickbite.quickbite.auth.service.SessionPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Background scheduler that periodically purges expired, revoked, or rotated
 * {@code RefreshToken} rows older than the configured retention window so the
 * {@code refresh_tokens} table does not grow unboundedly while preserving recent
 * history for token-reuse breach detection.
 */
@Component
public class RefreshTokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupScheduler.class);

    private final SessionPersistenceService sessionPersistenceService;
    private final int retentionDays;

    public RefreshTokenCleanupScheduler(
            SessionPersistenceService sessionPersistenceService,
            @Value("${quickbite.auth.stale-token-retention-days:30}") int retentionDays
    ) {
        this.sessionPersistenceService = sessionPersistenceService;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${quickbite.auth.cleanup-cron:0 0 3 * * *}")
    public void purgeStaleRefreshTokens() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(Duration.ofDays(retentionDays));
        try {
            int deleted = sessionPersistenceService.purgeStaleTokens(now, cutoff);
            if (deleted > 0) {
                log.info("Purged {} stale refresh tokens created before {}", deleted, cutoff);
            } else {
                log.debug("No stale refresh tokens found to purge before {}", cutoff);
            }
        } catch (Exception e) {
            log.error("Failed to purge stale refresh tokens: {}", e.getMessage(), e);
        }
    }
}
