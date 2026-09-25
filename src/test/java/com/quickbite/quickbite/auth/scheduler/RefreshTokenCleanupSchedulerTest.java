package com.quickbite.quickbite.auth.scheduler;

import com.quickbite.quickbite.auth.service.SessionPersistenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenCleanupScheduler")
class RefreshTokenCleanupSchedulerTest {

    @Mock
    private SessionPersistenceService sessionPersistenceService;

    @Test
    @DisplayName("purgeStaleRefreshTokens delegates to SessionPersistenceService with configured retention window")
    void purgeStaleRefreshTokens_delegatesWithCutoff() {
        RefreshTokenCleanupScheduler scheduler = new RefreshTokenCleanupScheduler(sessionPersistenceService, 30);
        when(sessionPersistenceService.purgeStaleTokens(any(), any())).thenReturn(15);

        scheduler.purgeStaleRefreshTokens();

        ArgumentCaptor<Instant> nowCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(sessionPersistenceService).purgeStaleTokens(nowCaptor.capture(), cutoffCaptor.capture());

        Duration diff = Duration.between(cutoffCaptor.getValue(), nowCaptor.getValue());
        assertThat(diff).isEqualTo(Duration.ofDays(30));
    }

    @Test
    @DisplayName("purgeStaleRefreshTokens swallows exceptions safely without crashing scheduler thread")
    void purgeStaleRefreshTokens_handlesExceptionsGracefully() {
        RefreshTokenCleanupScheduler scheduler = new RefreshTokenCleanupScheduler(sessionPersistenceService, 30);
        when(sessionPersistenceService.purgeStaleTokens(any(), any()))
                .thenThrow(new RuntimeException("DB unavailable"));

        assertThatCode(scheduler::purgeStaleRefreshTokens).doesNotThrowAnyException();
    }
}
