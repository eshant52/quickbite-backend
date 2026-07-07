package com.quickbite.quickbite.services;

import com.quickbite.quickbite.models.User;

import java.util.UUID;

public interface UserServiceI {
    User getUserById(UUID userId);

}
