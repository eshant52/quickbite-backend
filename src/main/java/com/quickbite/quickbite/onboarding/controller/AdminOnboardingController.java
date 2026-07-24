package com.quickbite.quickbite.onboarding.controller;

import com.quickbite.quickbite.onboarding.service.OnboardingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/onboard")
public class AdminOnboardingController {
    private final OnboardingService onboardingService;

    public AdminOnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<String> approve(@PathVariable("id") UUID id, @RequestParam(required = false) String remarks) {
        onboardingService.approveApplication(id, remarks);
        return ResponseEntity.ok("Application approved");
    }
}
