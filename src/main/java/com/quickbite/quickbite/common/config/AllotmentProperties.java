package com.quickbite.quickbite.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quickbite.admin.allotment")
public record AllotmentProperties(
        int maxAssignees
) {
    public AllotmentProperties {
        if (maxAssignees <= 0) {
            maxAssignees = 4;
        }
    }
}
