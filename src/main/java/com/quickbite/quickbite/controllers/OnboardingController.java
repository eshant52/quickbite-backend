package com.quickbite.quickbite.controllers;

import com.quickbite.quickbite.dtos.onboard.OnboardRestaurantRequest;
import com.quickbite.quickbite.dtos.onboard.OnboardRestaurantResponse;
import com.quickbite.quickbite.services.onboard.OnboardingService;
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
    public ResponseEntity<OnboardRestaurantResponse> submitApplication(@RequestBody OnboardRestaurantRequest request) {
        OnboardRestaurantResponse resp = onboardingService.submitRestaurantApplication(request);
        return ResponseEntity.ok(resp);
    }
}
