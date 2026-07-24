package com.quickbite.quickbite.onboarding.controller;

import com.quickbite.quickbite.onboarding.dto.OnboardRestaurantRequest;
import com.quickbite.quickbite.onboarding.dto.OnboardRestaurantResponse;
import com.quickbite.quickbite.onboarding.service.OnboardingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/onboard")
public class OnboardingController {
    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/restaurant")
    public ResponseEntity<OnboardRestaurantResponse> submitRestaurantApplication(@RequestBody OnboardRestaurantRequest request) {
        OnboardRestaurantResponse resp = onboardingService.submitRestaurantApplication(request);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/delivery-partner")
    public void submitDeliveryPartnerApplication(@RequestBody OnboardRestaurantRequest request) {

    }
}
