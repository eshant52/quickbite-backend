package com.quickbite.quickbite.services.auth;

import com.quickbite.quickbite.dtos.auth.AuthResponse;
import com.quickbite.quickbite.dtos.auth.DeviceInfo;
import com.quickbite.quickbite.dtos.auth.IssuedToken;
import com.quickbite.quickbite.dtos.auth.LoginRequest;
import com.quickbite.quickbite.dtos.auth.RegisterRequest;
import com.quickbite.quickbite.exceptions.AuthenticationException;
import com.quickbite.quickbite.models.User;
import com.quickbite.quickbite.models.UserRole;
import com.quickbite.quickbite.repositories.UserRepository;
import com.quickbite.quickbite.utils.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SessionService sessionService;
    private final SessionManagementTokenStoreService sessionManagementTokenStoreService;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                           SessionService sessionService, SessionManagementTokenStoreService sessionManagementTokenStoreService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.sessionService = sessionService;
        this.sessionManagementTokenStoreService = sessionManagementTokenStoreService;
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest, DeviceInfo deviceInfo) {
        User user = userRepository.findUserByEmail(loginRequest.email())
                .orElseThrow(
                        () -> new AuthenticationException("Invalid email or password"));

        if (!passwordEncoder.matches(loginRequest.password(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("Account is deactivated");
        }

        IssuedToken issuedToken = sessionService.createNewSession(user, deviceInfo);
        String accessToken = jwtUtil.generateAccessToken(user, issuedToken.familyId());

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return new AuthResponse(accessToken, issuedToken.rawToken(), issuedToken.familyId(), "Bearer");
    }

    @Override
    public AuthResponse refresh(String rawRefreshToken) {
        IssuedToken issuedRotatedToken = sessionService.validateAndRotate(rawRefreshToken);

        User user = issuedRotatedToken.user();
        String accessToken = jwtUtil.generateAccessToken(user, issuedRotatedToken.familyId());

        return new AuthResponse(accessToken, issuedRotatedToken.rawToken(), issuedRotatedToken.familyId(), "Bearer");
    }

    @Override
    public AuthResponse claimSession(String sessionManagementToken, DeviceInfo deviceInfo) {
        UUID userId = sessionManagementTokenStoreService.validateAndGetUserId(sessionManagementToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("Invalid or expired session management token"));

        IssuedToken issuedToken = sessionService.createNewSession(user, deviceInfo);
        sessionManagementTokenStoreService.invalidate(sessionManagementToken);

        String accessToken = jwtUtil.generateAccessToken(user, issuedToken.familyId());
        return new AuthResponse(accessToken, issuedToken.rawToken(), issuedToken.familyId(), "Bearer");
    }

    @Override
    public User register(RegisterRequest registerRequest) {
        userRepository.findUserByEmail(registerRequest.email()).ifPresent(_ -> {
            throw new AuthenticationException("Email is already registered");
        });

        User user = new User();
        user.setName(registerRequest.name());
        user.setEmail(registerRequest.email());
        user.setPhoneNumber(registerRequest.phoneNumber());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.password()));
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);

        return userRepository.save(user);
    }

    @Override
    public void logoutCurrentSession(UUID userId, UUID familyId) {
        sessionService.revokeSession(userId, familyId);
    }

    @Override
    public void logoutAllSessions(UUID userId) {
        sessionService.revokeAllSessions(userId);
    }
}
