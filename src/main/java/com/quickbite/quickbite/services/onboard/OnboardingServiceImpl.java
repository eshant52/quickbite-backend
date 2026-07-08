package com.quickbite.quickbite.services.onboard;

import com.quickbite.quickbite.dtos.onboard.OnboardRestaurantRequest;
import com.quickbite.quickbite.dtos.onboard.OnboardRestaurantResponse;
import com.quickbite.quickbite.models.*;
import com.quickbite.quickbite.repositories.*;
import com.quickbite.quickbite.services.auth.AuthService;
import com.quickbite.quickbite.exceptions.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OnboardingServiceImpl implements OnboardingService {
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantOnboardingRepository onboardingRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public OnboardingServiceImpl(UserRepository userRepository,
                                 RestaurantRepository restaurantRepository,
                                 RestaurantOnboardingRepository onboardingRepository,
                                 org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.onboardingRepository = onboardingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OnboardRestaurantResponse submitRestaurantApplication(OnboardRestaurantRequest request) {
        // Basic validation
        if (userRepository.findUserByEmail(request.ownerEmail()).isPresent()) {
            throw new AuthenticationException("Email already registered");
        }

        // Create user with role RESTAURANT but inactive
        User applicant = new User();
        applicant.setName(request.ownerName());
        applicant.setEmail(request.ownerEmail());
        applicant.setPhoneNumber(request.ownerPhone());
        // Hash password
        applicant.setPasswordHash(this.passwordEncoder.encode(request.password()));
        applicant.setRole(UserRole.RESTAURANT_OWNER);
        applicant.setActive(false);
        userRepository.save(applicant);

        // Create restaurant entity
        Restaurant r = new Restaurant();
        r.setOwner(applicant);
        r.setName(request.restaurantName());
        r.setDescription(request.description());
        r.setCurrentStatus(RestaurantVerificationStatus.PENDING);
        restaurantRepository.save(r);

        // Create onboarding application
        RestaurantOnboardingApplication app = new RestaurantOnboardingApplication();
        app.setApplicant(applicant);
        app.setRestaurant(r);
        app.setStatus(RestaurantOnboardingApplication.ApplicationStatus.PENDING);
        onboardingRepository.save(app);

        return new OnboardRestaurantResponse(app.getId(), "Application submitted; pending admin review");
    }

    @Override
    public void approveApplication(UUID applicationId, String reviewerRemarks) {
        RestaurantOnboardingApplication app = onboardingRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        app.setStatus(RestaurantOnboardingApplication.ApplicationStatus.APPROVED);
        app.setReviewRemarks(reviewerRemarks);
        app.setReviewedAt(java.time.Instant.now());
        // Activate user and restaurant
        User applicant = app.getApplicant();
        applicant.setActive(true);
        userRepository.save(applicant);

        Restaurant restaurant = app.getRestaurant();
        restaurant.setCurrentStatus(RestaurantVerificationStatus.APPROVED);
        restaurantRepository.save(restaurant);

        onboardingRepository.save(app);
    }
}
