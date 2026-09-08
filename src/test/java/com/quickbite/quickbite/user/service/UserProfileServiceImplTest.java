package com.quickbite.quickbite.user.service;

import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.user.dto.UpdateProfileRequest;
import com.quickbite.quickbite.user.dto.UserProfileResponse;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.UserRole;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setPhoneNumber("9876543210");
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("getProfile - returns user profile when user exists")
    void getProfile_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileResponse response = userProfileService.getProfile(userId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.name()).isEqualTo("Alice");
        assertThat(response.email()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("getProfile - throws ResourceNotFoundException when user does not exist")
    void getProfile_userNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getProfile(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("updateProfile - successfully updates name, email, phone")
    void updateProfile_success() {
        UpdateProfileRequest req = new UpdateProfileRequest("Alice Bob", "9998887776", "alice.bob@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("alice.bob@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("9998887776")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userProfileService.updateProfile(userId, req);

        assertThat(response.name()).isEqualTo("Alice Bob");
        assertThat(response.email()).isEqualTo("alice.bob@example.com");
        assertThat(response.phoneNumber()).isEqualTo("9998887776");
    }

    @Test
    @DisplayName("updateProfile - throws when new email already in use")
    void updateProfile_emailInUse() {
        UpdateProfileRequest req = new UpdateProfileRequest("Alice Bob", "9876543210", "taken@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userProfileService.updateProfile(userId, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Email already in use");
    }

    @Test
    @DisplayName("updateProfile - throws when new phone already in use")
    void updateProfile_phoneInUse() {
        UpdateProfileRequest req = new UpdateProfileRequest("Alice Bob", "1112223334", "alice@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneNumber("1112223334")).thenReturn(true);

        assertThatThrownBy(() -> userProfileService.updateProfile(userId, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Phone number already in use");
    }
}
