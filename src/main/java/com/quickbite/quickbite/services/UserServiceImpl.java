package com.quickbite.quickbite.services;

import com.quickbite.quickbite.exceptions.ResourceNotFoundException;
import com.quickbite.quickbite.models.User;
import com.quickbite.quickbite.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService implements UserServiceI {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User getUserById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
