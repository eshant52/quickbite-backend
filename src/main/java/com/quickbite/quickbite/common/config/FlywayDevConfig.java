package com.quickbite.quickbite.common.config;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * In the local {@code dev} profile, runs {@code flyway.repair()} prior to {@code flyway.migrate()}
 * so that renamed migration filenames/descriptions in {@code db/migration} are automatically
 * synchronized in {@code flyway_schema_history} on startup without manual database intervention.
 */
@Configuration
@Profile("dev")
public class FlywayDevConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
