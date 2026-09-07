package com.quickbite.quickbite.common.routing;

import java.util.List;

/**
 * Explicitly ordered routing providers used by {@link CompositeRoutingGateway}.
 */
public record RoutingProviderChain(List<RoutingGateway> providers) {

    public RoutingProviderChain {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalArgumentException("At least one routing provider must be configured");
        }
        providers = List.copyOf(providers);
    }
}
