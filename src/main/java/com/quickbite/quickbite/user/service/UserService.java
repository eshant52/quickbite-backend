package com.quickbite.quickbite.user.service;

import com.quickbite.quickbite.user.model.User;

import java.util.UUID;

public interface UserService {
    User getUserById(UUID userId);

}
