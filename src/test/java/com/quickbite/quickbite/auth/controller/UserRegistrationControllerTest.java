package com.quickbite.quickbite.auth.controller;

import com.quickbite.quickbite.auth.dto.RegisterRequest;
import com.quickbite.quickbite.auth.service.UserRegistrationService;
import com.quickbite.quickbite.user.dto.UserResponseDto;
import com.quickbite.quickbite.user.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationControllerTest {

    @Mock
    private UserRegistrationService userRegistrationService;

    @InjectMocks
    private UserRegistrationController controller;

    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest("John Doe", "john@example.com", "Password@123", "9876543210");
    }

    @Test
    @DisplayName("registerCustomer - returns 201 CREATED")
    void registerCustomer_success() {
        Instant now = Instant.now();
        UserResponseDto dto = new UserResponseDto(UUID.randomUUID(), "John Doe", "john@example.com", "9876543210", UserRole.CUSTOMER, true, now, now, now);
        when(userRegistrationService.registerCustomer(any(RegisterRequest.class))).thenReturn(dto);

        ResponseEntity<UserResponseDto> response = controller.registerCustomer(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(dto);
        verify(userRegistrationService).registerCustomer(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("registerDeliveryAgent - returns 201 CREATED")
    void registerDeliveryAgent_success() {
        Instant now = Instant.now();
        UserResponseDto dto = new UserResponseDto(UUID.randomUUID(), "John Doe", "john@example.com", "9876543210", UserRole.DELIVERY_AGENT, true, now, now, now);
        when(userRegistrationService.registerDeliveryAgent(any(RegisterRequest.class))).thenReturn(dto);

        ResponseEntity<UserResponseDto> response = controller.registerDeliveryAgent(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(dto);
        verify(userRegistrationService).registerDeliveryAgent(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("registerRestaurantOwner - returns 201 CREATED")
    void registerRestaurantOwner_success() {
        Instant now = Instant.now();
        UserResponseDto dto = new UserResponseDto(UUID.randomUUID(), "John Doe", "john@example.com", "9876543210", UserRole.RESTAURANT_OWNER, true, now, now, now);
        when(userRegistrationService.registerRestaurantOwner(any(RegisterRequest.class))).thenReturn(dto);

        ResponseEntity<UserResponseDto> response = controller.registerRestaurantOwner(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(dto);
        verify(userRegistrationService).registerRestaurantOwner(any(RegisterRequest.class));
    }
}
