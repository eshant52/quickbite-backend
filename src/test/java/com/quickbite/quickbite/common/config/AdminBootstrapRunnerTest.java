package com.quickbite.quickbite.common.config;

import com.quickbite.quickbite.common.config.property.AdminBootstrapProperties;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.UserRole;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationArguments applicationArguments;

    @Test
    @DisplayName("run - when bootstrapping is disabled, skips execution")
    void run_whenDisabled_skipsExecution() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties(false, "Super Admin", "admin@qb.com", "+100", "pass");
        AdminBootstrapRunner runner = new AdminBootstrapRunner(userRepository, passwordEncoder, properties);

        runner.run(applicationArguments);

        verify(userRepository, never()).findByRole(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("run - when admin already exists, does nothing")
    void run_whenAdminAlreadyExists_doesNothing() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties(true, "Super Admin", "admin@qb.com", "+100", "pass");
        AdminBootstrapRunner runner = new AdminBootstrapRunner(userRepository, passwordEncoder, properties);

        User existingAdmin = new User();
        existingAdmin.setRole(UserRole.ADMIN);
        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of(existingAdmin));

        runner.run(applicationArguments);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("run - when no admin exists, creates super admin")
    void run_whenNoAdminExists_createsSuperAdmin() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties(true, "Super Admin", "admin@qb.com", "+10000000000", "Secret@123");
        AdminBootstrapRunner runner = new AdminBootstrapRunner(userRepository, passwordEncoder, properties);

        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of());
        when(userRepository.existsByEmailIgnoreCase("admin@qb.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+10000000000")).thenReturn(false);
        when(passwordEncoder.encode("Secret@123")).thenReturn("encodedPassword");

        runner.run(applicationArguments);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Super Admin");
        assertThat(saved.getEmail()).isEqualTo("admin@qb.com");
        assertThat(saved.getPhoneNumber()).isEqualTo("+10000000000");
        assertThat(saved.getPasswordHash()).isEqualTo("encodedPassword");
        assertThat(saved.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName("run - when user with admin email already exists, skips creating")
    void run_whenEmailExists_skipsCreating() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties(true, "Super Admin", "admin@qb.com", "+10000000000", "Secret@123");
        AdminBootstrapRunner runner = new AdminBootstrapRunner(userRepository, passwordEncoder, properties);

        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of());
        when(userRepository.existsByEmailIgnoreCase("admin@qb.com")).thenReturn(true);

        runner.run(applicationArguments);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("run - when user with admin phone number already exists, skips creating")
    void run_whenPhoneExists_skipsCreating() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties(true, "Super Admin", "admin@qb.com", "+10000000000", "Secret@123");
        AdminBootstrapRunner runner = new AdminBootstrapRunner(userRepository, passwordEncoder, properties);

        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of());
        when(userRepository.existsByEmailIgnoreCase("admin@qb.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+10000000000")).thenReturn(true);

        runner.run(applicationArguments);

        verify(userRepository, never()).save(any());
    }
}
