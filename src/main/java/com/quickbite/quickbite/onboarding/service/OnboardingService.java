package com.quickbite.quickbite.onboarding.service;

import com.quickbite.quickbite.onboarding.dto.OnboardRestaurantRequest;
import com.quickbite.quickbite.onboarding.dto.OnboardRestaurantResponse;

import java.util.UUID;

public interface OnboardingService {
    OnboardRestaurantResponse submitRestaurantApplication(OnboardRestaurantRequest request);
    void approveApplication(UUID applicationId, String reviewerRemarks);
}
