package com.quickbite.quickbite.exceptions;

import lombok.Getter;

@Getter
public class MaxSessionException extends RuntimeException {
    private final String sessionManagementToken;
    private final int maxSessions;

    public MaxSessionException(String sessionManagementToken, int maxSessions) {
        super("Maximum concurrent sessions reached");
        this.sessionManagementToken = sessionManagementToken;
        this.maxSessions = maxSessions;
    }

}
