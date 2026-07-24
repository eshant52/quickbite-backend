package com.quickbite.quickbite.user.controller;

import com.quickbite.quickbite.user.dto.UserResponseDto;
import com.quickbite.quickbite.user.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer")
public class CustomerController {
    private final UserService userService;

    public CustomerController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public UserResponseDto getUser(@AuthenticationPrincipal Jwt jwt) {
        return UserResponseDto.toDto(
                userService.getUserById(
                        UUID.fromString(Objects.requireNonNull(jwt.getSubject()))
                )
        );
    }
}
