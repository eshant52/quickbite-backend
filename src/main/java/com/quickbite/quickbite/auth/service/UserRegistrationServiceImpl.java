package com.quickbite.quickbite.auth.service;

import com.quickbite.quickbite.auth.dto.RegisterRequest;
import com.quickbite.quickbite.auth.exception.AuthenticationException;
import com.quickbite.quickbite.user.dto.UserResponseDto;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.UserRole;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserRegistrationServiceImpl implements UserRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponseDto registerCustomer(RegisterRequest registerRequest) {
        User user = registerUser(registerRequest);
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);

        return UserResponseDto.toDto(userRepository.save(user));
    }

    @Override
    public UserResponseDto registerRestaurantOwner(RegisterRequest registerRequest) {
        User user = registerUser(registerRequest);
        user.setRole(UserRole.RESTAURANT_OWNER);
        user.setActive(false);

        return UserResponseDto.toDto(userRepository.save(user));
    }

    @Override
    public UserResponseDto registerDeliveryAgent(RegisterRequest registerRequest) {
        User user = registerUser(registerRequest);
        user.setRole(UserRole.DELIVERY_AGENT);
        user.setActive(false);

        return UserResponseDto.toDto(userRepository.save(user));
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
