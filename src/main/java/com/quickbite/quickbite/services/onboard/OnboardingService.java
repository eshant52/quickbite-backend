package com.quickbite.quickbite.services.onboard;

import com.quickbite.quickbite.dtos.onboard.OnboardRestaurantRequest;
import com.quickbite.quickbite.dtos.onboard.OnboardRestaurantResponse;

import java.util.UUID;

public interface OnboardingService {
    OnboardRestaurantResponse submitRestaurantApplication(OnboardRestaurantRequest request);
    void approveApplication(UUID applicationId, String reviewerRemarks);
}
