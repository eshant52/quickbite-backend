package com.quickbite.quickbite.user.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.user.dto.UpdateProfileRequest;
import com.quickbite.quickbite.user.dto.UserProfileResponse;
import com.quickbite.quickbite.user.model.UserRole;
import com.quickbite.quickbite.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerProfileControllerTest {

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private AuthenticatedSessionResolver authenticatedSessionResolver;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private CustomerProfileController controller;

    private UUID userId;
    private UserProfileResponse mockProfile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockProfile = new UserProfileResponse(
                userId,
                "Alice",
                "9876543210",
                "alice@example.com",
                UserRole.CUSTOMER,
                true,
                Instant.now()
        );
    }

    @Test
    @DisplayName("getProfile - returns 200 OK with user profile")
    void getProfile_success() {
        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);
        when(userProfileService.getProfile(userId)).thenReturn(mockProfile);

        ResponseEntity<UserProfileResponse> response = controller.getProfile(jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockProfile);
    }

    @Test
    @DisplayName("updateProfile - returns 200 OK with updated profile")
    void updateProfile_success() {
        UpdateProfileRequest request = new UpdateProfileRequest("Alice Updated", "9876543210", "alice.new@example.com");
        UserProfileResponse updatedProfile = new UserProfileResponse(
                userId,
                "Alice Updated",
                "9876543210",
                "alice@example.com",
                UserRole.CUSTOMER,
                true,
                Instant.now()
        );

        when(authenticatedSessionResolver.userIdFromJwt(jwt)).thenReturn(userId);
        when(userProfileService.updateProfile(userId, request)).thenReturn(updatedProfile);

        ResponseEntity<UserProfileResponse> response = controller.updateProfile(jwt, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(updatedProfile);
        verify(userProfileService).updateProfile(userId, request);
    }
}
