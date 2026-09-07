package com.quickbite.quickbite.common.routing.exception;

import com.quickbite.quickbite.common.routing.CompositeRoutingGateway;

/**
 * Thrown by a routing adapter when the remote provider is unreachable,
 * returns a 5xx error, or times out.
 *
 * <p>The {@link CompositeRoutingGateway} catches this and falls through
 * to the next provider in the chain.
 */
public class RoutingProviderUnavailableException extends RuntimeException {

    public RoutingProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public RoutingProviderUnavailableException(String message) {
        super(message);
    }

    public static RoutingProviderUnavailableException fromProvider(String provider, Throwable cause) {
        return new RoutingProviderUnavailableException(
                "Routing provider [" + provider + "] is unavailable: " + cause.getMessage(), cause);
    }
}
