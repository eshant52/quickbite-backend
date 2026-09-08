package com.quickbite.quickbite.auth.service;

import com.quickbite.quickbite.auth.dto.RegisterRequest;
import com.quickbite.quickbite.auth.exception.AuthenticationException;
import com.quickbite.quickbite.user.dto.UserResponseDto;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.UserRole;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserRegistrationServiceImpl userRegistrationService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest(
                "John Doe",
                "john@example.com",
                "9876543210",
                "password123"
        );
    }

    @Test
    @DisplayName("registerCustomer - successfully registers customer as active")
    void registerCustomer_success() {
        when(userRepository.findUserByEmail(registerRequest.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setCreatedAt(Instant.now());
            return u;
        });

        UserResponseDto response = userRegistrationService.registerCustomer(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("John Doe");
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.role()).isEqualTo(UserRole.CUSTOMER);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(savedUser.isActive()).isTrue();
        assertThat(savedUser.getPasswordHash()).isEqualTo("encodedPassword");
    }

    @Test
    @DisplayName("registerRestaurant - successfully registers restaurant owner as inactive pending onboarding")
    void registerRestaurant_success() {
        when(userRepository.findUserByEmail(registerRequest.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setCreatedAt(Instant.now());
            return u;
        });

        UserResponseDto response = userRegistrationService.registerRestaurant(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.role()).isEqualTo(UserRole.RESTAURANT_OWNER);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(UserRole.RESTAURANT_OWNER);
        assertThat(savedUser.isActive()).isFalse();
    }

    @Test
    @DisplayName("registerDeliveryPartner - successfully registers delivery partner as inactive pending onboarding")
    void registerDeliveryPartner_success() {
        when(userRepository.findUserByEmail(registerRequest.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setCreatedAt(Instant.now());
            return u;
        });

        UserResponseDto response = userRegistrationService.registerDeliveryPartner(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.role()).isEqualTo(UserRole.DELIVERY_AGENT);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(UserRole.DELIVERY_AGENT);
        assertThat(savedUser.isActive()).isFalse();
    }

    @Test
    @DisplayName("registerCustomer - duplicate email throws AuthenticationException")
    void register_duplicateEmail_throwsAuthenticationException() {
        when(userRepository.findUserByEmail(registerRequest.email())).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userRegistrationService.registerCustomer(registerRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Email is already registered");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}
