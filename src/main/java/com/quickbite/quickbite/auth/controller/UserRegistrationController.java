package com.quickbite.quickbite.auth.controller;

import com.quickbite.quickbite.auth.dto.RegisterRequest;
import com.quickbite.quickbite.auth.service.UserRegistrationService;
import com.quickbite.quickbite.user.dto.UserResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/register")
public class UserRegistrationController {

    private final UserRegistrationService userRegistrationService;

    public UserRegistrationController(UserRegistrationService userRegistrationService) {
        this.userRegistrationService = userRegistrationService;
    }

    @PostMapping("/customer")
    public ResponseEntity<UserResponseDto> registerCustomer(@RequestBody @Valid RegisterRequest registerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userRegistrationService.registerCustomer(RegisterRequest.xssValidate(registerRequest)));
    }

    @PostMapping("/delivery-partner")
    public ResponseEntity<UserResponseDto> registerDeliveryPartner(@RequestBody @Valid RegisterRequest registerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userRegistrationService.registerDeliveryPartner(RegisterRequest.xssValidate(registerRequest)));
    }

    @PostMapping("/restaurant")
    public ResponseEntity<UserResponseDto> registerRestaurant(@RequestBody @Valid RegisterRequest registerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userRegistrationService.registerRestaurant(RegisterRequest.xssValidate(registerRequest)));
    }
}
