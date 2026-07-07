package com.quickbite.quickbite.services;

import com.quickbite.quickbite.models.User;

import java.util.UUID;

public interface UserService {
    User getUserById(UUID userId);

}
