package com.quickbite.quickbite.auth.service;

import com.quickbite.quickbite.auth.dto.*;
import com.quickbite.quickbite.user.dto.UserResponseDto;
import com.quickbite.quickbite.user.model.User;

import java.util.List;
import java.util.UUID;

public interface AuthService {
    UserResponseDto registerCustomer(RegisterRequest registerRequest);

    UserResponseDto registerDeliveryPartner(RegisterRequest registerRequest);

    UserResponseDto registerRestaurant(RegisterRequest registerRequest);

    AuthResponse login(LoginRequest loginRequest, DeviceInfo deviceInfo);

    List<SessionResponse> getActiveSessionsForUser(UUID userId);

    void revokeSession(UUID userId, UUID sessionId);

    AuthResponse claimSession(UUID userId, DeviceInfo deviceInfo);

    AuthResponse refresh(String rawRefreshToken);

    void logoutCurrentSession(UUID userId, UUID sessionId);

    void logoutAllSessions(UUID userId);
}
