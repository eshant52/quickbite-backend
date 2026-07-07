package com.quickbite.quickbite.controllers;

import com.quickbite.quickbite.dtos.UserResponseDto;
import com.quickbite.quickbite.services.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping()
    public UserResponseDto getUser(@AuthenticationPrincipal Jwt jwt) {
        return UserResponseDto.toDto(userService.getUserById(UUID.fromString(jwt.getSubject())));
    }
}
