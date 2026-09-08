package com.quickbite.quickbite.user.service;

import com.quickbite.quickbite.user.dto.UpdateProfileRequest;
import com.quickbite.quickbite.user.dto.UserProfileResponse;

import java.util.UUID;

/**
 * Service dedicated to user profile management.
 */
public interface UserProfileService {

    UserProfileResponse getProfile(UUID userId);

    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest req);
}
