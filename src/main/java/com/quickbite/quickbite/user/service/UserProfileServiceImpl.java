package com.quickbite.quickbite.user.service;

import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.user.dto.UpdateProfileRequest;
import com.quickbite.quickbite.user.dto.UserProfileResponse;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;

    public UserProfileServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = loadUser(userId);
        return UserProfileResponse.from(user);
    }

    @Override
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = loadUser(userId);

        if (!req.email().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new ResourceNotFoundException("Email already in use");
        }

        if (!req.phoneNumber().equalsIgnoreCase(user.getPhoneNumber())
                && userRepository.existsByPhoneNumber(req.phoneNumber())) {
            throw new ResourceNotFoundException("Phone number already in use");
        }

        user.setName(req.name());
        user.setPhoneNumber(req.phoneNumber());
        user.setEmail(req.email());
        return UserProfileResponse.from(userRepository.save(user));
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
