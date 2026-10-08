package com.quickbite.quickbite.common.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quickbite.bootstrap.admin")
public record AdminBootstrapProperties(
        boolean enabled,
        String name,
        String email,
        String phoneNumber,
        String password
) {
    public AdminBootstrapProperties {
        if (name == null || name.isBlank()) {
            name = "Super Admin";
        }
        if (email == null || email.isBlank()) {
            email = "admin@quickbite.com";
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            phoneNumber = "+10000000000";
        }
        if (password == null || password.isBlank()) {
            password = "Admin@123456";
        }
    }
}
