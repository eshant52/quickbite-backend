package com.quickbite.quickbite.services.onboard;

import com.quickbite.quickbite.dtos.onboard.OnboardRestaurantRequest;
import com.quickbite.quickbite.dtos.onboard.OnboardRestaurantResponse;
import com.quickbite.quickbite.models.*;
import com.quickbite.quickbite.repositories.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OnboardingServiceImpl implements OnboardingService {
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;

    public OnboardingServiceImpl(UserRepository userRepository,
                                 RestaurantRepository restaurantRepository) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Override
    public OnboardRestaurantResponse submitRestaurantApplication(OnboardRestaurantRequest request) {
        // 1. Add restaurant information
        // 2. Restaurant address
        // 3. Restaurant document
        // 4. Restaurant hours
        // 5. Restaurant image upload

        // 6. Send verification details to admins. (Optional)
        return new OnboardRestaurantResponse(null, "Application submitted; pending admin review");
    }

    @Override
    public void approveApplication(UUID applicationId, String reviewerRemarks) {

    }
}
