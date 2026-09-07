package com.quickbite.quickbite.common.config;

import com.quickbite.quickbite.common.routing.RoutingProviderChain;
import com.quickbite.quickbite.common.routing.adapter.HaversineFallbackAdapter;
import com.quickbite.quickbite.common.routing.adapter.MapboxRoutingAdapter;
import com.quickbite.quickbite.common.routing.adapter.OsrmRoutingAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Configures the active routing chain using Spring Profiles.
 *
 * <ul>
 *   <li><b>dev / default</b>: Local OSRM instance with Haversine fallback.</li>
 *   <li><b>prod</b>: Mapbox API with Haversine fallback. OSRM is strictly omitted in prod.</li>
 * </ul>
 */
@Configuration
public class RoutingConfig {

    /**
     * Development profile (and default for test environments):
     * Uses self-hosted OSRM with Haversine fallback.
     */
    @Bean
    @Profile({"dev", "default"})
    public RoutingProviderChain devRoutingChain(
            OsrmRoutingAdapter osrm,
            HaversineFallbackAdapter haversine
    ) {
        return new RoutingProviderChain(List.of(osrm, haversine));
    }

    /**
     * Production profile:
     * Uses Mapbox Directions/Matrix API with Haversine fallback.
     * OSRM is not configured, injected, or needed in production.
     */
    @Bean
    @Profile("prod")
    public RoutingProviderChain prodRoutingChain(
            MapboxRoutingAdapter mapbox,
            HaversineFallbackAdapter haversine
    ) {
        return new RoutingProviderChain(List.of(mapbox, haversine));
    }
}
