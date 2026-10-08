package com.quickbite.quickbite.user.service;

import com.quickbite.quickbite.common.exception.ResourceConflictException;
import com.quickbite.quickbite.user.dto.CreateAdminRequest;
import com.quickbite.quickbite.user.dto.UserResponseDto;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.UserRole;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponseDto createAdmin(CreateAdminRequest request) {
        userRepository.findUserByEmail(request.email())
                .ifPresent(_ -> {
                    throw new ResourceConflictException("Email is already registered");
                });

        userRepository.findUserByPhoneNumber(request.phoneNumber())
                .ifPresent(_ -> {
                    throw new ResourceConflictException("Phone number is already registered");
                });

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.ADMIN);
        user.setActive(true);

        User saved = userRepository.save(user);
        log.info("[ADMIN] Successfully created new administrator: id={}, email={}", saved.getId(), saved.getEmail());

        return UserResponseDto.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> listAdmins() {
        return userRepository.findByRole(UserRole.ADMIN)
                .stream()
                .map(UserResponseDto::toDto)
                .toList();
    }
}
