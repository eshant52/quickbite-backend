package com.quickbite.quickbite.auth.service;

import com.quickbite.quickbite.auth.dto.AuthResponse;
import com.quickbite.quickbite.auth.dto.DeviceInfo;
import com.quickbite.quickbite.auth.dto.LoginRequest;

/**
 * Service dedicated to authentication entrypoints: credential login and refresh-token rotation.
 */
public interface AuthenticationService {

    AuthResponse login(LoginRequest loginRequest, DeviceInfo deviceInfo);

    AuthResponse refresh(String rawRefreshToken);
}
