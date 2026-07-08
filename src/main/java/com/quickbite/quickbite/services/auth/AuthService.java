package com.quickbite.quickbite.services.auth;

import com.quickbite.quickbite.dtos.auth.AuthResponse;
import com.quickbite.quickbite.dtos.auth.DeviceInfo;
import com.quickbite.quickbite.dtos.auth.LoginRequest;
import com.quickbite.quickbite.dtos.auth.RegisterRequest;
import com.quickbite.quickbite.models.User;

import java.util.UUID;

public interface AuthService {
    AuthResponse login(LoginRequest loginRequest, DeviceInfo deviceInfo);
    AuthResponse refresh(String rawRefreshToken);
    AuthResponse claimSession(String sessionManagementToken, DeviceInfo deviceInfo);
    User registerCustomer(RegisterRequest registerRequest);
    User registerDeliveryPartner(RegisterRequest registerRequest);
    User registerRestaurant(RegisterRequest registerRequest);
    void logoutCurrentSession(UUID userId, UUID familyId);
    void logoutAllSessions(UUID userId);
}
