package com.quickbite.quickbite.common.config;

import com.quickbite.quickbite.common.routing.CompositeRoutingGateway;
import com.quickbite.quickbite.common.routing.RoutingGateway;
import com.quickbite.quickbite.common.routing.RoutingProviderChain;
import com.quickbite.quickbite.common.routing.adapter.HaversineFallbackAdapter;
import com.quickbite.quickbite.common.routing.adapter.MapboxRoutingAdapter;
import com.quickbite.quickbite.common.routing.adapter.OsrmRoutingAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RoutingConfigTest {

    private RoutingConfig routingConfig;

    @Mock
    private OsrmRoutingAdapter osrm;

    @Mock
    private MapboxRoutingAdapter mapbox;

    private HaversineFallbackAdapter haversine;

    @BeforeEach
    void setUp() {
        routingConfig = new RoutingConfig();
        haversine = new HaversineFallbackAdapter();
    }

    @Test
    @DisplayName("Dev routing chain contains OSRM followed by Haversine")
    void devRoutingChain_containsOsrmAndHaversine() {
        RoutingProviderChain chain = routingConfig.devRoutingChain(osrm, haversine);

        List<RoutingGateway> providers = chain.providers();
        assertThat(providers).containsExactly(osrm, haversine);

        // Verify CompositeRoutingGateway can be instantiated without circular references
        CompositeRoutingGateway composite = new CompositeRoutingGateway(chain);
        assertThat(composite).isNotNull();
    }

    @Test
    @DisplayName("Prod routing chain contains Mapbox followed by Haversine and strictly NO OSRM")
    void prodRoutingChain_containsMapboxAndHaversineOnly() {
        RoutingProviderChain chain = routingConfig.prodRoutingChain(mapbox, haversine);

        List<RoutingGateway> providers = chain.providers();
        assertThat(providers).containsExactly(mapbox, haversine);
        assertThat(providers).doesNotContain(osrm);

        CompositeRoutingGateway composite = new CompositeRoutingGateway(chain);
        assertThat(composite).isNotNull();
    }
}
