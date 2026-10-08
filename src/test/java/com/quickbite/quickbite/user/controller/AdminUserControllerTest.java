package com.quickbite.quickbite.user.controller;

import com.quickbite.quickbite.user.dto.CreateAdminRequest;
import com.quickbite.quickbite.user.dto.UserResponseDto;
import com.quickbite.quickbite.user.model.UserRole;
import com.quickbite.quickbite.user.service.AdminUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    @DisplayName("POST /api/v1/admin/users/admins delegates to AdminUserService and returns 201 Created")
    void createAdmin_delegatesAndReturnsCreated() {
        CreateAdminRequest request = new CreateAdminRequest(
                "Admin User",
                "admin@quickbite.com",
                "+1234567890",
                "Password@123"
        );

        UserResponseDto responseDto = new UserResponseDto(
                UUID.randomUUID(),
                "Admin User",
                "admin@quickbite.com",
                "+1234567890",
                UserRole.ADMIN,
                true,
                null,
                Instant.now(),
                Instant.now()
        );

        when(adminUserService.createAdmin(any(CreateAdminRequest.class))).thenReturn(responseDto);

        ResponseEntity<UserResponseDto> response = adminUserController.createAdmin(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(responseDto);
        verify(adminUserService).createAdmin(any(CreateAdminRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/admin/users/admins delegates to AdminUserService and returns 200 OK")
    void listAdmins_delegatesAndReturnsOk() {
        UserResponseDto responseDto = new UserResponseDto(
                UUID.randomUUID(),
                "Admin User",
                "admin@quickbite.com",
                "+1234567890",
                UserRole.ADMIN,
                true,
                null,
                Instant.now(),
                Instant.now()
        );

        when(adminUserService.listAdmins()).thenReturn(List.of(responseDto));

        ResponseEntity<List<UserResponseDto>> response = adminUserController.listAdmins();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(responseDto);
        verify(adminUserService).listAdmins();
    }
}
