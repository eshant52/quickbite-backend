package com.quickbite.quickbite.common.config;

import com.quickbite.quickbite.common.routing.RoutingGateway;
import com.quickbite.quickbite.common.routing.RoutingProviderChain;
import com.quickbite.quickbite.common.routing.adapter.HaversineFallbackAdapter;
import com.quickbite.quickbite.common.routing.adapter.MapboxRoutingAdapter;
import com.quickbite.quickbite.common.routing.adapter.OsrmRoutingAdapter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

/**
 * Configures the active routing chain using Spring Profiles.
 *
 * <ul>
 *   <li><b>dev / default</b>: Configurable chain supporting local OSRM and/or Mapbox with Haversine fallback.</li>
 *   <li><b>prod</b>: Mapbox API with Haversine fallback. OSRM is strictly omitted in prod.</li>
 * </ul>
 */
@Configuration
public class RoutingConfig {

    /**
     * Development profile (and default for test environments):
     * Dynamically resolves active routing adapters (OSRM and/or Mapbox)
     * based on enabled properties, always backed by Haversine as final fallback.
     */
    @Bean
    @Profile({"dev", "default"})
    public RoutingProviderChain devRoutingChain(
            ObjectProvider<OsrmRoutingAdapter> osrmProvider,
            ObjectProvider<MapboxRoutingAdapter> mapboxProvider,
            HaversineFallbackAdapter haversine
    ) {
        List<RoutingGateway> providers = new ArrayList<>();
        osrmProvider.ifAvailable(providers::add);
        mapboxProvider.ifAvailable(providers::add);
        providers.add(haversine);
        return new RoutingProviderChain(providers);
    }

    /**
     * Programmatic / test helper for constructing a dev routing chain directly.
     */
    public RoutingProviderChain devRoutingChain(
            OsrmRoutingAdapter osrm,
            HaversineFallbackAdapter haversine
    ) {
        List<RoutingGateway> providers = new ArrayList<>();
        if (osrm != null) {
            providers.add(osrm);
        }
        providers.add(haversine);
        return new RoutingProviderChain(providers);
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
