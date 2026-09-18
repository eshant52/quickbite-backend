package com.quickbite.quickbite.common.config;

import com.quickbite.quickbite.common.config.property.RedissonProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates a {@link RedissonClient} bean for distributed locking.
 *
 * <p>Redisson manages its own connection pool alongside {@code spring-data-redis} (Lettuce).
 * Both point to the same Redis instance but use independent connection pools —
 * Lettuce is used for caching/session; Redisson is used exclusively for distributed locks.
 *
 * <p>Configuration is sourced from {@link RedissonProperties}
 * ({@code quickbite.redisson.*} in {@code application.properties}).
 */
@Configuration
public class RedissonConfig {

    private final RedissonProperties properties;

    public RedissonConfig(RedissonProperties properties) {
        this.properties = properties;
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + properties.host() + ":" + properties.port())
                .setConnectionPoolSize(properties.connectionPoolSize())
                .setConnectionMinimumIdleSize(properties.connectionMinimumIdleSize());
        return Redisson.create(config);
    }
}
