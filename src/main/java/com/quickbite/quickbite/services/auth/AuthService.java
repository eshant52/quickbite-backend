package com.quickbite.quickbite.services;

import com.quickbite.quickbite.dtos.auth.AuthResponseDto;
import com.quickbite.quickbite.dtos.auth.DeviceInfo;
import com.quickbite.quickbite.dtos.auth.LoginRequestDto;
import com.quickbite.quickbite.dtos.auth.RegisterRequestDto;
import com.quickbite.quickbite.models.User;

import java.util.UUID;

public interface AuthService {
    AuthResponseDto login(LoginRequestDto loginRequestDto, DeviceInfo deviceInfo);
    AuthResponseDto refresh(String rawRefreshToken);
    AuthResponseDto claimSession(String sessionManagementToken, DeviceInfo deviceInfo);
    User register(RegisterRequestDto registerRequestDto);
    void logoutCurrentSession(UUID userId, UUID familyId);
    void logoutAllSessions(UUID userId);
}
