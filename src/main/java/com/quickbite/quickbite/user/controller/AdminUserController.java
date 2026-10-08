package com.quickbite.quickbite.user.controller;

import com.quickbite.quickbite.user.dto.CreateAdminRequest;
import com.quickbite.quickbite.user.dto.UserResponseDto;
import com.quickbite.quickbite.user.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PostMapping("/admins")
    public ResponseEntity<UserResponseDto> createAdmin(@RequestBody @Valid CreateAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminUserService.createAdmin(CreateAdminRequest.xssValidate(request)));
    }

    @GetMapping("/admins")
    public ResponseEntity<List<UserResponseDto>> listAdmins() {
        return ResponseEntity.ok(adminUserService.listAdmins());
    }
}
