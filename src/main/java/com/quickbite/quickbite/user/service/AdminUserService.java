package com.quickbite.quickbite.user.service;

import com.quickbite.quickbite.user.dto.CreateAdminRequest;
import com.quickbite.quickbite.user.dto.UserResponseDto;

import java.util.List;

public interface AdminUserService {

    UserResponseDto createAdmin(CreateAdminRequest request);

    List<UserResponseDto> listAdmins();
}
