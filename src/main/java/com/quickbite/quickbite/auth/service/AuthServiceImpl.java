package com.quickbite.quickbite.auth.service;

import com.quickbite.quickbite.auth.dto.*;
import com.quickbite.quickbite.auth.exception.AuthenticationException;
import com.quickbite.quickbite.auth.service.token.AccessTokenService;
import com.quickbite.quickbite.user.dto.UserResponseDto;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.UserRole;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final SessionService sessionService;


    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AccessTokenService accessTokenService,
            SessionService sessionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.sessionService = sessionService;
    }


    @Override
    public UserResponseDto registerCustomer(RegisterRequest registerRequest) {
        User user = registerUser(registerRequest);
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);

        return UserResponseDto.toDto(userRepository.save(user));
    }


    @Override
    public UserResponseDto registerRestaurant(RegisterRequest registerRequest) {
        User user = registerUser(registerRequest);
        user.setRole(UserRole.RESTAURANT_OWNER);
        user.setActive(false);

        return UserResponseDto.toDto(userRepository.save(user));
    }


    @Override
    public UserResponseDto registerDeliveryPartner(RegisterRequest registerRequest) {
        User user = registerUser(registerRequest);
        user.setRole(UserRole.DELIVERY_AGENT);
        user.setActive(false);

        return UserResponseDto.toDto(userRepository.save(user));
    }


    @Override
    public AuthResponse login(LoginRequest loginRequest, DeviceInfo deviceInfo) {
        User user = userRepository.findUserByEmail(loginRequest.email())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (!passwordEncoder.matches(loginRequest.password(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("Account is deactivated");
        }

        IssuedToken issuedToken = sessionService.createNewSession(user, deviceInfo);
        String accessToken = accessTokenService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                issuedToken.sessionId()
        );

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return new AuthResponse(
                accessToken,
                issuedToken.rawToken(),
                accessTokenService.defaultExpirationDurationInSeconds(),
                issuedToken.sessionId()
        );
    }

    @Override
    public List<SessionResponse> getActiveSessionsForUser(UUID userId) {
        return sessionService.listActiveSessionsForUser(userId);
    }

    @Override
    public void revokeSession(UUID userId, UUID sessionId) {
        sessionService.revokeSession(userId, sessionId);
    }


    @Override
    public AuthResponse claimSession(UUID userId, DeviceInfo deviceInfo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("Invalid or expired session management token"));

        IssuedToken issuedToken = sessionService.createNewSession(user, deviceInfo);

        String accessToken = accessTokenService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                issuedToken.sessionId());

        return new AuthResponse(
                accessToken,
                issuedToken.rawToken(),
                accessTokenService.defaultExpirationDurationInSeconds(),
                issuedToken.sessionId()
        );
    }


    @Override
    public AuthResponse refresh(String rawRefreshToken) {
        IssuedToken issuedRotatedToken = sessionService.validateAndRotate(rawRefreshToken);

        UUID userId = issuedRotatedToken.userId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found for refresh token"));

        String accessToken = accessTokenService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                issuedRotatedToken.sessionId()
        );

        return new AuthResponse(
                accessToken,
                issuedRotatedToken.rawToken(),
                accessTokenService.defaultExpirationDurationInSeconds(),
                issuedRotatedToken.sessionId()
        );
    }


    @Override
    public void logoutCurrentSession(UUID userId, UUID sessionId) {
        sessionService.revokeSession(userId, sessionId);
    }


    @Override
    public void logoutAllSessions(UUID userId) {
        sessionService.revokeAllSessions(userId);
    }


    private User registerUser(RegisterRequest registerRequest) {
        userRepository.findUserByEmail(registerRequest.email())
                .ifPresent(_ -> {
                    throw new AuthenticationException("Email is already registered");
                });

        User user = new User();
        user.setName(registerRequest.name());
        user.setEmail(registerRequest.email());
        user.setPhoneNumber(registerRequest.phoneNumber());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.password()));
        user.setActive(false);

        return user;
    }
}
