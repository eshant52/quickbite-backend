package com.quickbite.quickbite.common.config;

import com.quickbite.quickbite.common.config.property.AdminBootstrapProperties;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.UserRole;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminBootstrapProperties properties;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AdminBootstrapProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            log.info("[BOOTSTRAP] Admin bootstrapping is disabled.");
            return;
        }

        if (userRepository.findByRole(UserRole.ADMIN).isEmpty()) {
            log.info("[BOOTSTRAP] No administrator found. Bootstrapping initial super admin...");

            if (userRepository.existsByEmailIgnoreCase(properties.email())) {
                log.warn("[BOOTSTRAP] User with email {} already exists with non-admin role. Skipping auto-creation.",
                        properties.email());
                return;
            }

            if (userRepository.existsByPhoneNumber(properties.phoneNumber())) {
                log.warn("[BOOTSTRAP] User with phone {} already exists. Skipping auto-creation.",
                        properties.phoneNumber());
                return;
            }

            User admin = new User();
            admin.setName(properties.name());
            admin.setEmail(properties.email());
            admin.setPhoneNumber(properties.phoneNumber());
            admin.setPasswordHash(passwordEncoder.encode(properties.password()));
            admin.setRole(UserRole.ADMIN);
            admin.setActive(true);

            userRepository.save(admin);
            log.info("[BOOTSTRAP] Initial super-admin successfully created with email: {}", properties.email());
        } else {
            log.debug("[BOOTSTRAP] Administrator accounts already exist. Skipping root admin initialization.");
        }
    }
}
